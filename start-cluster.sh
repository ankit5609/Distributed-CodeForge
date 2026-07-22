#!/bin/bash

echo "🟢 Starting CodeForge Stateless Microservices (1 replica each)..."
kubectl scale deployment account-service intelligence-service workspace-service codeforge-frontend --replicas=1 -n codeforge-core

echo "🟢 Starting Preview Runner Pool (1 replica)..."
kubectl scale deployment runner-pool --replicas=1 -n codeforge-previews

echo "✅ Project is waking up! Use 'kubectl get pods -A' to monitor the startup process."
