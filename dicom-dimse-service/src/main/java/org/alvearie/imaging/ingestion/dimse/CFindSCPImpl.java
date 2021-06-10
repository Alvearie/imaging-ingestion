/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.alvearie.imaging.ingestion.dimse;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.alvearie.imaging.ingestion.model.result.DicomAttribute;
import org.alvearie.imaging.ingestion.model.result.DicomEntityResult;
import org.alvearie.imaging.ingestion.model.result.DicomQueryModel;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Attributes.Visitor;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.data.Value;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Dimse;
import org.dcm4che3.net.QueryOption;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.pdu.ExtendedNegotiation;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicQueryTask;
import org.dcm4che3.net.service.DicomServiceException;
import org.dcm4che3.net.service.QueryRetrieveLevel2;

import org.dcm4che3.net.service.QueryTask;
import org.dcm4che3.util.TagUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CFindSCPImpl extends AbstractDicomService {

    private static final Logger LOG = Logger.getLogger(CFindSCPImpl.class);

    // Supported retrieval levels
    private static final EnumSet<QueryRetrieveLevel2> QUERY_LEVELS = EnumSet.of(QueryRetrieveLevel2.STUDY,
            QueryRetrieveLevel2.SERIES, QueryRetrieveLevel2.IMAGE);

    @Inject
    @RestClient
    QueryRetrieveClient queryClient;

    @ConfigProperty(name = "provider.name")
    String source;

    public CFindSCPImpl() {
        super(UID.StudyRootQueryRetrieveInformationModelFind);
    }

    public CFindSCPImpl(String... sopClasses) {
        super(sopClasses);
    }

    @Override
    public void onDimseRQ(Association as, PresentationContext pc, Dimse dimse, Attributes rq, Attributes keys)
            throws IOException {
        if (dimse != Dimse.C_FIND_RQ)
            throw new DicomServiceException(Status.UnrecognizedOperation);

        QueryTask queryTask = calculateMatches(as, pc, rq, keys);
        as.getApplicationEntity().getDevice().execute(queryTask);
    }

    protected QueryTask calculateMatches(Association as, PresentationContext pc, Attributes rq, Attributes data)
            throws DicomServiceException {

        if (isRelational(as, rq)) {
            LOG.error("Relational C-FIND negotiated but not supported");
            throw new DicomServiceException(400); 
        }

        QueryRetrieveLevel2 queryLevel = QueryRetrieveLevel2.validateQueryIdentifier(data, QUERY_LEVELS, false);
        if (queryLevel != null) {
            DicomEntityQueryTask task = new DicomEntityQueryTask(as, pc, rq, data);

            switch (queryLevel) {
            case PATIENT:
                throw new UnsupportedOperationException();
            case STUDY:
                task.setScope(DicomQueryModel.Scope.STUDY);
                break;
            case SERIES:
                task.setScope(DicomQueryModel.Scope.SERIES);
                break;
            case IMAGE:
                task.setScope(DicomQueryModel.Scope.INSTANCE);
                break;
            default:
                LOG.error("Unexpected C-FIND LEVEL");
                throw new DicomServiceException(400);
            }
            return task;

        }
        LOG.error("Unsupported C-FIND LEVEL");
        throw new DicomServiceException(400); 
    }

    @SuppressWarnings("unused")
    private boolean isRelational(Association as, Attributes rq) {
        ExtendedNegotiation extendedNegotiation = as.getAAssociateAC()
                .getExtNegotiationFor(rq.getString(Tag.AffectedSOPClassUID));
        return QueryOption.toOptions(extendedNegotiation).contains(QueryOption.RELATIONAL);

    }

    class DicomEntityQueryTask extends BasicQueryTask {

        Iterator<DicomEntityResult> resultIterator;
        DicomQueryModel.Scope scope = null;

        public DicomEntityQueryTask(Association as, PresentationContext pc, Attributes rq, Attributes keys) {
            super(as, pc, rq, keys);
        }

        @Override
        public void run() {
            executeQuery();
            super.run();
        }

        @Override
        public boolean hasMoreMatches() throws DicomServiceException {
            return resultIterator.hasNext();
        }

        @Override
        public Attributes nextMatch() throws DicomServiceException {
            Attributes attributes = entityResultToAttributes(resultIterator.next());
            return attributes;
        }

        public void setScope(DicomQueryModel.Scope scope) {
            this.scope = scope;

        }

        protected void executeQuery() {
            DicomQueryModel model = new DicomQueryModel();
            model.setScope(this.scope);
            model.setQueryAttributes(attributesToQueryAttributes(keys));

            List<DicomEntityResult> queryResults = queryClient.getResults(model, source);
            LOG.info(String.format("C-FIND %s with %d matches from provider %s", scope.toString(), queryResults.size(),
                    source));
            resultIterator = queryResults.iterator();
        }

        private Attributes entityResultToAttributes(DicomEntityResult entityResult) {
            if (entityResult == null) {
                return null;
            }
            Set<Entry<String, DicomAttribute>> resultAttributes = entityResult.getAttributes().entrySet();
            final Attributes attributes = new Attributes(resultAttributes.size());
            resultAttributes.forEach(resultAttribute -> {
                List<String> value = resultAttribute.getValue().getValue();
                VR vr = VR.valueOf(resultAttribute.getValue().getVr());
                if (value != null) {
                    attributes.setString(TagUtils.forName(resultAttribute.getKey()), vr,
                            (String[]) value.toArray(new String[] {}));
                } else {
                    attributes.setValue(TagUtils.forName(resultAttribute.getKey()), vr, Value.NULL);
                }
            });
            return attributes;
        }

        protected Map<Integer, String> attributesToQueryAttributes(Attributes keys) {
            Map<Integer, String> queryAttributes = new HashMap<Integer, String>();
            // Walk the attributes and build the query attributes
            try {
                keys.accept(new Visitor() {
                    @Override
                    public boolean visit(Attributes attrs, int tag, VR vr, Object value) throws Exception {
                        // Everything that is being queried on should be convertible to a string type
                        if (value != null && value != Value.NULL) {
                            try {
                                if (vr.isStringType()) {
                                    queryAttributes.put(tag, (String) vr.toStrings(value, false, null));
                                }
                            } catch (UnsupportedOperationException e) {
                                LOG.warn(String.format(
                                        "Unsupported key attribute with TAG %s, VR %s, and value class %s",
                                        TagUtils.toHexString(tag), vr.name(), value.getClass().getName()));
                            }
                        }
                        return true;
                    }

                }, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return queryAttributes;
        }
    }
}
