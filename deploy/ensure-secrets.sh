#!/bin/sh
# Skaffold before-deploy hook: creates each missing Secret in the namespace and context of the deploy. The databases,
# the Keycloak admin and the API client secret get random values; the demo users get the same passwords as in Docker
# Compose (viewer, editor) unless DEMO_VIEWER_PASSWORD / DEMO_EDITOR_PASSWORD are set, which a publicly reachable
# cluster should do. Never changes an existing Secret: PostgreSQL keeps the password of its first start.
set -eu

KUBECTL="${KUBECTL:-kubectl}"
NAMESPACE="${SKAFFOLD_NAMESPACES:-}"
NAMESPACE="${NAMESPACE%%,*}"
SCOPE=""
[ -n "${SKAFFOLD_KUBE_CONTEXT:-}" ] && SCOPE="$SCOPE --context $SKAFFOLD_KUBE_CONTEXT"
[ -n "$NAMESPACE" ] && SCOPE="$SCOPE --namespace $NAMESPACE"

# ensure <name> <key=value>...
ensure() {
    name=$1
    shift
    if "$KUBECTL" $SCOPE get secret "$name" > /dev/null 2>&1; then
        echo "Secret $name exists, unchanged"
        return
    fi
    literals=""
    for pair in "$@"; do
        literals="$literals --from-literal=$pair"
    done
    # shellcheck disable=SC2086 # the literals are split on purpose; no value contains a space
    "$KUBECTL" $SCOPE create secret generic "$name" $literals > /dev/null
    "$KUBECTL" $SCOPE label secret "$name" app.kubernetes.io/part-of=order-management > /dev/null
    echo "Secret $name created"
}

random() {
    openssl rand -hex 16
}

for name in postgres-command postgres-query postgres-reference; do
    ensure "$name" username=orders "password=$(random)"
done
ensure keycloak-admin username=admin "password=$(random)"
ensure order-management-oidc-client "client-secret=$(random)"
ensure order-management-demo-users \
    "viewer-password=${DEMO_VIEWER_PASSWORD:-viewer}" "editor-password=${DEMO_EDITOR_PASSWORD:-editor}"
