variable "aws_region" {
  type        = string
  description = "AWS region for the Terraform state bucket."
  default     = "ap-northeast-2"
}

variable "project" {
  type        = string
  description = "Project tag for newly created backend resources."
  default     = "moru"
}

variable "environment" {
  type        = string
  description = "Environment tag for newly created backend resources."
  default     = "prod"
}

variable "state_bucket_name" {
  type        = string
  description = "Globally unique name for the dedicated Terraform state bucket."
  nullable    = false
}

variable "state_key" {
  type        = string
  description = "Object key used by the production Terraform backend."
  default     = "prod/terraform.tfstate"
}
