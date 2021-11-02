#!/bin/bash -ex

SCP_NAME=dimse-proxy-scp
SCP_KS_PASSWORD=secret
SCP_TS_PASSWORD=secret

SCU_NAME=dimse-proxy-scu
SCU_KS_PASSWORD=secret
SCU_TS_PASSWORD=secret

# Generate SCP Cert Key pair
openssl req -x509 -nodes -days 3650 -newkey rsa:2048 \
    -keyout ${SCP_NAME}.key -out ${SCP_NAME}.crt -subj "/CN=localhost"

# Create SCP Keystore in PKCS12 format
openssl pkcs12 -export -out ${SCP_NAME}-keystore.pkcs12 -in ${SCP_NAME}.crt \
    -inkey ${SCP_NAME}.key \
    -passout pass:${SCP_KS_PASSWORD}

# Create SCP Truststore
keytool -import -noprompt -trustcacerts -keystore ${SCP_NAME}-truststore.pkcs12 -file ${SCP_NAME}.crt \
    -storepass ${SCP_TS_PASSWORD}


# Generate SCU Cert Key pair
openssl req -x509 -nodes -days 3650 -newkey rsa:2048 \
          -keyout ${SCU_NAME}.key -out ${SCU_NAME}.crt -subj "/CN=localhost"

# Create SCU Keystore in PKCS12 format
openssl pkcs12 -export -out ${SCU_NAME}-keystore.pkcs12 -in ${SCU_NAME}.crt \
    -inkey ${SCU_NAME}.key \
    -passout pass:${SCU_KS_PASSWORD}

# Create SCU Truststore
keytool -import -noprompt -trustcacerts -keystore ${SCU_NAME}-truststore.pkcs12 -file ${SCU_NAME}.crt \
    -storepass ${SCU_TS_PASSWORD}
