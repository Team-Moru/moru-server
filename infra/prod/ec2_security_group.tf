resource "aws_security_group" "moru_ec2" {
  name        = "moru-ec2-sg"
  description = "launch-wizard-2 created 2026-07-12T14:22:52.206Z"
  vpc_id      = data.aws_vpc.existing.id

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_vpc_security_group_ingress_rule" "moru_ec2_ssh_ipv4" {
  security_group_id = aws_security_group.moru_ec2.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 22
  ip_protocol       = "tcp"
  to_port           = 22
}

resource "aws_vpc_security_group_ingress_rule" "moru_ec2_http_ipv4" {
  security_group_id = aws_security_group.moru_ec2.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 80
  ip_protocol       = "tcp"
  to_port           = 80
}

resource "aws_vpc_security_group_ingress_rule" "moru_ec2_http_ipv6" {
  security_group_id = aws_security_group.moru_ec2.id
  cidr_ipv6         = "::/0"
  from_port         = 80
  ip_protocol       = "tcp"
  to_port           = 80
}

resource "aws_vpc_security_group_ingress_rule" "moru_ec2_https_ipv4" {
  security_group_id = aws_security_group.moru_ec2.id
  cidr_ipv4         = "0.0.0.0/0"
  from_port         = 443
  ip_protocol       = "tcp"
  to_port           = 443
}

resource "aws_vpc_security_group_ingress_rule" "moru_ec2_https_ipv6" {
  security_group_id = aws_security_group.moru_ec2.id
  cidr_ipv6         = "::/0"
  from_port         = 443
  ip_protocol       = "tcp"
  to_port           = 443
}

resource "aws_vpc_security_group_ingress_rule" "moru_ec2_application_test" {
  security_group_id = aws_security_group.moru_ec2.id
  cidr_ipv4         = "58.29.146.38/32"
  from_port         = 8080
  ip_protocol       = "tcp"
  to_port           = 8080
}

resource "aws_vpc_security_group_egress_rule" "moru_ec2_all_ipv4" {
  security_group_id = aws_security_group.moru_ec2.id
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}
