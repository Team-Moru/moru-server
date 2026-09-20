resource "aws_security_group" "moru_rds" {
  name        = "moru-rds-sg"
  description = "Created by RDS management console"
  vpc_id      = data.aws_vpc.existing.id

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_vpc_security_group_ingress_rule" "moru_rds_all_ipv4" {
  security_group_id = aws_security_group.moru_rds.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

resource "aws_vpc_security_group_ingress_rule" "moru_rds_all_ipv6" {
  security_group_id = aws_security_group.moru_rds.id
  cidr_ipv6         = "::/0"
  ip_protocol       = "-1"
}

resource "aws_vpc_security_group_egress_rule" "moru_rds_all_ipv4" {
  security_group_id = aws_security_group.moru_rds.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}
