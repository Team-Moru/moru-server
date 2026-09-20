data "aws_caller_identity" "current" {}

data "aws_vpc" "existing" {
  id = var.vpc_id
}

data "aws_subnet" "production_ec2" {
  id = var.ec2_subnet_id
}

check "expected_aws_account" {
  assert {
    condition     = data.aws_caller_identity.current.account_id == var.aws_account_id
    error_message = "This configuration is restricted to AWS account ${var.aws_account_id}."
  }
}
