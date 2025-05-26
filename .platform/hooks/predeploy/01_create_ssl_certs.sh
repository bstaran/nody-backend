#!/bin/bash

set -e

echo "Fetching SSL certificate and key from AWS Parameter Store."

CERT_PARAM_NAME="/nody/production/CF_ORIGIN_CERT"
KEY_PARAM_NAME="/nody/production/CF_ORIGIN_KEY"
CA_PARAM_NAME="/nody/production/CF_ORIGIN_PULL_CA"

echo "Checking AWS region (using IMDSv2)."
TOKEN=$(curl -s -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
AWS_REGION=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/placement/region)
if [ -z "$AWS_REGION" ]; then
    echo "Error: Could not determine AWS region from instance metadata (IMDSv2)."
    exit 1
fi
echo "Region confirmed: $AWS_REGION"


echo "Fetching certificate from Parameter Store (parameter: $CERT_PARAM_NAME)..."
                                    
CERT_CONTENT=$(aws ssm get-parameter --name "$CERT_PARAM_NAME" --region "$AWS_REGION" --query "Parameter.Value" --output text)

if [ -z "$CERT_CONTENT" ]; then
    echo "Error: Failed to fetch certificate from Parameter Store or the parameter value is empty."
    echo "Please check if the parameter '$CERT_PARAM_NAME' exists in the '$AWS_REGION' region and if the instance has the required permissions."
    exit 1
fi

echo "Creating certificate file..."
mkdir -p /etc/pki/tls/certs
echo "$CERT_CONTENT" > /etc/pki/tls/certs/cloudflare-origin.pem
echo "Certificate file created at /etc/pki/tls/certs/cloudflare-origin.pem."


echo "Fetching private key from Parameter Store (parameter: $KEY_PARAM_NAME)..."

KEY_CONTENT=$(aws ssm get-parameter --name "$KEY_PARAM_NAME" --with-decryption --region "$AWS_REGION" --query "Parameter.Value" --output text)

if [ -z "$KEY_CONTENT" ]; then
    echo "Error: Failed to fetch private key from Parameter Store or the parameter value is empty."
    echo "Please check if the parameter '$KEY_PARAM_NAME' exists in the '$AWS_REGION' region and if the instance has the required permissions."
    exit 1
fi

echo "Creating private key file..."
mkdir -p /etc/pki/tls/private
echo "$KEY_CONTENT" > /etc/pki/tls/private/cloudflare-origin-key.pem
echo "Private key file created at /etc/pki/tls/private/cloudflare-origin-key.pem."


echo "Setting permissions for the private key file..."
chmod 600 /etc/pki/tls/private/cloudflare-origin-key.pem


echo "Fetching Cloudflare Origin Pull CA certificate from Parameter Store (parameter: $CA_PARAM_NAME)..."

CA_CONTENT=$(aws ssm get-parameter --name "$CA_PARAM_NAME" --region "$AWS_REGION" --query "Parameter.Value" --output text)

if [ -z "$CA_CONTENT" ]; then
    echo "Error: Failed to fetch CA certificate from Parameter Store or the parameter value is empty."
    echo "Please check if the parameter '$CA_PARAM_NAME' exists in the '$AWS_REGION' region and if the instance has the required permissions."
    exit 1
fi

echo "Creating Cloudflare Origin Pull CA certificate file..."
mkdir -p /etc/pki/tls/certs
echo "$CA_CONTENT" > /etc/pki/tls/certs/cloudflare-ca.pem
echo "CA certificate file created at /etc/pki/tls/certs/cloudflare-ca.pem."


echo "SSL setup using AWS Parameter Store completed successfully."
