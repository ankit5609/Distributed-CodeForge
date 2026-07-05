#!/bin/bash

echo "🛑 Stopping CodeForge Stateless Microservices..."
kubectl scale deployment account-service intelligence-service workspace-service codeforge-frontend --replicas=0 -n codeforge-core

echo "🛑 Stopping Preview Runner Pool..."
kubectl scale deployment runner-pool --replicas=0 -n codeforge-previews

echo "✅ Project is now hibernated. Databases and MinIO are still preserved and running!"
