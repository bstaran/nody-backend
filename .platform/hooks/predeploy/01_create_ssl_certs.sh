#!/bin/bash

set -e

echo "Creating SSL certificate and key files from environment variables..."

if [ -z "$CF_ORIGIN_CERT" ]; then
    echo "Error: CF_ORIGIN_CERT environment variable is not set."
    exit 1
fi
mkdir -p /etc/pki/tls/certs
echo "$CF_ORIGIN_CERT" | base64 -d > /etc/pki/tls/certs/cloudflare-origin.pem

if [ -z "$CF_ORIGIN_KEY" ]; then
    echo "Error: CF_ORIGIN_KEY environment variable is not set."
    exit 1
fi
mkdir -p /etc/pki/tls/private
echo "$CF_ORIGIN_KEY" | base64 -d > /etc/pki/tls/private/cloudflare-origin-key.pem

chmod 600 /etc/pki/tls/private/cloudflare-origin-key.pem

echo "SSL files created successfully."
