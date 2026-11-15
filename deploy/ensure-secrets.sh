#!/bin/sh
# Skaffold before-deploy hook: creates each missing database Secret with a random password, in the namespace and
# context of the deploy. Never changes an existing Secret: PostgreSQL keeps the password of its first start.
set -eu

KUBECTL="${KUBECTL:-kubectl}"
NAMESPACE="${SKAFFOLD_NAMESPACES:-}"
NAMESPACE="${NAMESPACE%%,*}"
set --
[ -n "${SKAFFOLD_KUBE_CONTEXT:-}" ] && set -- "$@" --context "$SKAFFOLD_KUBE_CONTEXT"
[ -n "$NAMESPACE" ] && set -- "$@" --namespace "$NAMESPACE"

for name in postgres-command postgres-query postgres-reference; do
    if "$KUBECTL" "$@" get secret "$name" > /dev/null 2>&1; then
        echo "Secret $name exists, unchanged"
    else
        "$KUBECTL" "$@" create secret generic "$name" \
            --from-literal=username=orders \
            --from-literal=password="$(openssl rand -hex 16)"
        "$KUBECTL" "$@" label secret "$name" app.kubernetes.io/part-of=order-management > /dev/null
    fi
done
