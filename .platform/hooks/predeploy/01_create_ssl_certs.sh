#!/bin/bash

set -e

echo "Creating SSL certificate and key files from environment variables..."

CERT_CONTENT="$CF_ORIGIN_CERT"
KEY_CONTENT="$CF_ORIGIN_KEY"

if [ -z "$CERT_CONTENT" ]; then
    echo "Error: CF_ORIGIN_CERT environment variable is not set or empty. Check Elastic Beanstalk > Configuration > Software settings."
    exit 1
fi
mkdir -p /etc/pki/tls/certs
echo "$CERT_CONTENT" > /etc/pki/tls/certs/cloudflare-origin.pem

if [ -z "$KEY_CONTENT" ]; then
    echo "Error: CF_ORIGIN_KEY environment variable is not set or empty. Check Elastic Beanstalk > Configuration > Software settings."
    exit 1
fi
mkdir -p /etc/pki/tls/private
echo "$KEY_CONTENT" > /etc/pki/tls/private/cloudflare-origin-key.pem

chmod 600 /etc/pki/tls/private/cloudflare-origin-key.pem

echo "SSL files created successfully from environment variables."
