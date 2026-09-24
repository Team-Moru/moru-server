import {
  to = aws_security_group.moru_ec2
  id = var.ec2_security_group_id
}

import {
  to = aws_vpc_security_group_ingress_rule.moru_ec2_ssh_ipv4
  id = "sgr-004e04b488a69a503"
}

import {
  to = aws_vpc_security_group_ingress_rule.moru_ec2_http_ipv4
  id = "sgr-01059589aaad7c6a1"
}

import {
  to = aws_vpc_security_group_ingress_rule.moru_ec2_http_ipv6
  id = "sgr-03bb52b4064251e44"
}

import {
  to = aws_vpc_security_group_ingress_rule.moru_ec2_https_ipv4
  id = "sgr-0a3f486457056be64"
}

import {
  to = aws_vpc_security_group_ingress_rule.moru_ec2_https_ipv6
  id = "sgr-0e861ece375b623f2"
}

import {
  to = aws_vpc_security_group_ingress_rule.moru_ec2_application_test
  id = "sgr-0f864a686e6cd7576"
}

import {
  to = aws_vpc_security_group_egress_rule.moru_ec2_all_ipv4
  id = "sgr-0a32ac6e70b28569a"
}

import {
  to = aws_iam_role.moru_server
  id = var.ec2_iam_role_name
}

import {
  to = aws_iam_role_policy.moru_dev_assets
  id = "moru-server-s3-role:moru-dev-assets-policy"
}

import {
  to = aws_iam_role_policy.moru_prod_assets
  id = "moru-server-s3-role:moru-prod-assets-policy"
}

import {
  to = aws_iam_instance_profile.moru_server
  id = "moru-server-s3-role"
}

import {
  to = aws_instance.moru_server
  id = var.ec2_instance_id
}

import {
  to = aws_eip.moru_server
  id = var.ec2_eip_allocation_id
}

import {
  to = aws_security_group.moru_rds
  id = var.rds_security_group_id
}

import {
  to = aws_vpc_security_group_ingress_rule.moru_rds_all_ipv4
  id = "sgr-0c65f28523cfde22d"
}

import {
  to = aws_vpc_security_group_ingress_rule.moru_rds_all_ipv6
  id = "sgr-0d3eba8272521a11d"
}

import {
  to = aws_vpc_security_group_egress_rule.moru_rds_all_ipv4
  id = "sgr-010c3ce6d12b7f436"
}

import {
  to = aws_db_parameter_group.moru
  id = "korean"
}

import {
  to = aws_db_instance.moru
  id = var.rds_instance_identifier
}

import {
  to = aws_s3_bucket.production_assets
  id = var.production_assets_bucket_name
}

import {
  to = aws_s3_bucket_public_access_block.production_assets
  id = var.production_assets_bucket_name
}

import {
  to = aws_s3_bucket_ownership_controls.production_assets
  id = var.production_assets_bucket_name
}

import {
  to = aws_s3_bucket_server_side_encryption_configuration.production_assets
  id = var.production_assets_bucket_name
}

import {
  to = aws_s3_bucket.production_preview
  id = var.production_preview_bucket_name
}

import {
  to = aws_s3_bucket_public_access_block.production_preview
  id = var.production_preview_bucket_name
}

import {
  to = aws_s3_bucket_ownership_controls.production_preview
  id = var.production_preview_bucket_name
}

import {
  to = aws_s3_bucket_server_side_encryption_configuration.production_preview
  id = var.production_preview_bucket_name
}

import {
  to = aws_s3_bucket_policy.production_preview
  id = var.production_preview_bucket_name
}

import {
  to = aws_cloudwatch_log_group.application
  id = var.application_log_group_name
}
