variable "aws_region" {
  type        = string
  description = "Region that contains the existing production resources."
  default     = "ap-northeast-2"
}

variable "aws_account_id" {
  type        = string
  description = "AWS account that owns the existing production resources."
  default     = "488230509502"
}

variable "vpc_id" {
  type        = string
  description = "Existing default VPC. Terraform must only reference it."
  default     = "vpc-087c2d76939191694"
}

variable "ec2_subnet_id" {
  type        = string
  description = "Existing default public subnet used by the production EC2 instance."
  default     = "subnet-0a94acc68b254e59f"
}

variable "ec2_instance_id" {
  type        = string
  description = "Existing Moru production EC2 instance ID."
  default     = "i-0b4fe94824ca7d3c7"
}

variable "ec2_eip_allocation_id" {
  type        = string
  description = "Existing Elastic IP allocation associated with the production EC2 instance."
  default     = "eipalloc-0423074c40c068102"
}

variable "ec2_security_group_id" {
  type        = string
  description = "Existing security group attached to the production EC2 instance."
  default     = "sg-0ff632924dad2adb6"
}

variable "ec2_iam_role_name" {
  type        = string
  description = "Existing EC2 role that grants S3 and CloudWatch access."
  default     = "moru-server-s3-role"
}
