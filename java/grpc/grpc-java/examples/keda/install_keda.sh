#!/bin/bash

export KEDACORE_HELM_REPOSITORY=kedacore
export KEDACORE_HELM_REPOSITORY_URL=https://$KEDACORE_HELM_REPOSITORY.github.io/charts
export KEDA_NAMESPACE=keda

helm repo add $KEDACORE_HELM_REPOSITORY $KEDACORE_HELM_REPOSITORY_URL
helm repo update

helm install keda $KEDACORE_HELM_REPOSITORY/keda --namespace $KEDA_NAMESPACE --create-namespace
