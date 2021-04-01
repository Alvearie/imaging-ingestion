/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package common

import (
	"context"
	"fmt"

	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
)

var log = logf.Log.WithName("controlleraction")

type DesiredResourceState []ControllerAction

type ActionRunner interface {
	RunAll(desiredState DesiredResourceState) error
	Create(obj client.Object) error
	Update(obj client.Object) error
	Error(err error) error
}

type ControllerAction interface {
	Run(runner ActionRunner) (string, error)
}

type ControllerActionRunner struct {
	client  client.Client
	context context.Context
	scheme  *runtime.Scheme
	cr      client.Object
}

func (d *DesiredResourceState) AddAction(action ControllerAction) DesiredResourceState {
	if action != nil {
		*d = append(*d, action)
	}
	return *d
}

func (d *DesiredResourceState) AddActions(actions []ControllerAction) DesiredResourceState {
	for _, action := range actions {
		*d = d.AddAction(action)
	}
	return *d
}

// NewControllerActionRunner creates an action runner to run kubernetes actions
func NewControllerActionRunner(context context.Context, client client.Client, scheme *runtime.Scheme, cr client.Object) ActionRunner {
	return &ControllerActionRunner{
		client:  client,
		context: context,
		scheme:  scheme,
		cr:      cr,
	}
}

func (i *ControllerActionRunner) RunAll(desiredState DesiredResourceState) error {
	for index, action := range desiredState {
		msg, err := action.Run(i)
		if err != nil {
			log.Info(fmt.Sprintf("(%5d) %10s %s", index, "FAILED", msg))
			return err
		}
		log.Info(fmt.Sprintf("(%5d) %10s %s", index, "SUCCESS", msg))
	}

	return nil
}

func (i *ControllerActionRunner) Create(obj client.Object) error {
	err := controllerutil.SetControllerReference(i.cr.(v1.Object), obj.(v1.Object), i.scheme)
	if err != nil {
		log.Error(err, "Error setting controller reference")
		return err
	}

	err = i.client.Create(i.context, obj)
	if err != nil {
		log.Error(err, "Error creating object")
		return err
	}

	return nil
}

func (i *ControllerActionRunner) Update(obj client.Object) error {
	err := controllerutil.SetControllerReference(i.cr.(v1.Object), obj.(v1.Object), i.scheme)
	if err != nil {
		log.Error(err, "Error setting controller reference")
		return err
	}

	err = i.client.Update(i.context, obj)
	if err != nil {
		log.Error(err, "Error updating object")
		return err
	}

	return nil
}

func (i *ControllerActionRunner) Error(err error) error {
	return err
}

// An action to create generic kubernetes resources
// (resources that don't require special treatment)
type GenericCreateAction struct {
	Ref client.Object
	Msg string
}

// An action to update generic kubernetes resources
// (resources that don't require special treatment)
type GenericUpdateAction struct {
	Ref client.Object
	Msg string
}

// An action to return error
type GenericErrorAction struct {
	Ref error
	Msg string
}

func (i GenericCreateAction) Run(runner ActionRunner) (string, error) {
	return i.Msg, runner.Create(i.Ref)
}

func (i GenericUpdateAction) Run(runner ActionRunner) (string, error) {
	return i.Msg, runner.Update(i.Ref)
}

func (i GenericErrorAction) Run(runner ActionRunner) (string, error) {
	return i.Msg, runner.Error(i.Ref)
}
