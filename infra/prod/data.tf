data "aws_caller_identity" "current" {
  lifecycle {
    postcondition {
      condition     = self.account_id == var.aws_account_id
      error_message = "This configuration is restricted to AWS account ${var.aws_account_id}."
    }
  }
}

data "aws_vpc" "existing" {
  id = var.vpc_id
}

data "aws_subnet" "production_ec2" {
  id = var.ec2_subnet_id
}

data "aws_db_subnet_group" "existing" {
  name = var.rds_subnet_group_name
}
