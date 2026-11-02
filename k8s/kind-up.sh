#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

CLUSTER=order-platform
REGISTRY=localhost:8083

if ! kind get clusters | grep -qx "$CLUSTER"; then
  kind create cluster --config k8s/kind-config.yaml
fi

for node in $(kind get nodes --name "$CLUSTER"); do
  docker exec "$node" mkdir -p "/etc/containerd/certs.d/$REGISTRY"
  printf '[host."http://nexus:8083"]\n  capabilities = ["pull", "resolve"]\n' \
    | docker exec -i "$node" tee "/etc/containerd/certs.d/$REGISTRY/hosts.toml" >/dev/null
done

for service in nexus jenkins; do
  container=$(docker compose --profile ci ps -q "$service" || true)
  if [ -n "$container" ]; then
    docker network disconnect kind "$container" 2>/dev/null || true
    docker network connect --alias "$service" kind "$container"
  fi
done

touch .env
grep -v '^KUBECONFIG_B64=' .env > .env.tmp || true
mv .env.tmp .env
echo "KUBECONFIG_B64=$(kind get kubeconfig --internal --name "$CLUSTER" | base64 | tr -d '\n')" >> .env

echo "Cluster $CLUSTER is ready. Recreate Jenkins to pick up the kubeconfig:"
echo "  docker compose --profile ci up -d jenkins"
