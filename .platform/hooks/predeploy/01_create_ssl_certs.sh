#!/bin/bash

set -x
set -e

echo "AWS Parameter Store에서 SSL 인증서와 키를 가져옵니다."

CERT_PARAM_NAME="/nody/production/CF_ORIGIN_CERT"
KEY_PARAM_NAME="/nody/production/CF_ORIGIN_KEY"

echo "AWS 리전을 확인합니다."
AWS_REGION=$(curl -s http://169.254.169.254/latest/meta-data/placement/availability-zone | sed 's/\(.*\)[a-z]/\1/')
if [ -z "$AWS_REGION" ]; then
    echo "오류: 인스턴스 메타데이터에서 AWS 리전을 확인할 수 없습니다."
    exit 1
fi
echo "리전 확인됨: $AWS_REGION"


echo "Parameter Store에서 인증서를 가져옵니다 (파라미터: $CERT_PARAM_NAME)..."
                                    
CERT_CONTENT=$(aws ssm get-parameter --name "$CERT_PARAM_NAME" --region "$AWS_REGION" --query "Parameter.Value" --output text)

if [ -z "$CERT_CONTENT" ]; then
    echo "오류: Parameter Store에서 인증서를 가져오지 못했거나 파라미터 값이 비어있습니다."
    echo "'$AWS_REGION' 리전에 '$CERT_PARAM_NAME' 파라미터가 존재하는지, 그리고 인스턴스에 권한이 있는지 확인하세요."
    exit 1
fi

echo "인증서 파일을 생성합니다..."
mkdir -p /etc/pki/tls/certs
echo "$CERT_CONTENT" > /etc/pki/tls/certs/cloudflare-origin.pem
echo "인증서 파일이 /etc/pki/tls/certs/cloudflare-origin.pem 경로에 생성되었습니다."


echo "Parameter Store에서 개인 키를 가져옵니다 (파라미터: $KEY_PARAM_NAME)..."

KEY_CONTENT=$(aws ssm get-parameter --name "$KEY_PARAM_NAME" --with-decryption --region "$AWS_REGION" --query "Parameter.Value" --output text)

if [ -z "$KEY_CONTENT" ]; then
    echo "오류: Parameter Store에서 개인 키를 가져오지 못했거나 파라미터 값이 비어있습니다."
    echo "'$AWS_REGION' 리전에 '$KEY_PARAM_NAME' 파라미터가 존재하는지, 그리고 인스턴스에 권한이 있는지 확인하세요."
    exit 1
fi

echo "개인 키 파일을 생성합니다..."
mkdir -p /etc/pki/tls/private
echo "$KEY_CONTENT" > /etc/pki/tls/private/cloudflare-origin-key.pem
echo "개인 키 파일이 /etc/pki/tls/private/cloudflare-origin-key.pem 경로에 생성되었습니다."


echo "개인 키 파일의 권한을 설정합니다..."
chmod 600 /etc/pki/tls/private/cloudflare-origin-key.pem

echo "AWS Parameter Store를 이용한 SSL 설정이 성공적으로 완료되었습니다."
