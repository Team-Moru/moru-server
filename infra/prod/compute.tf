resource "aws_instance" "moru_server" {
  ami                    = "ami-0e4ab31f1847c850c"
  instance_type          = "t3.micro"
  key_name               = "moru-key"
  subnet_id              = data.aws_subnet.production_ec2.id
  private_ip             = "172.31.37.85"
  vpc_security_group_ids = [aws_security_group.moru_ec2.id]
  iam_instance_profile   = aws_iam_instance_profile.moru_server.name

  ebs_optimized     = true
  monitoring        = false
  source_dest_check = true

  metadata_options {
    http_endpoint               = "enabled"
    http_protocol_ipv6          = "disabled"
    http_put_response_hop_limit = 2
    http_tokens                 = "required"
    instance_metadata_tags      = "disabled"
  }

  root_block_device {
    delete_on_termination = true
    encrypted             = true
    iops                  = 3000
    kms_key_id            = "arn:aws:kms:ap-northeast-2:488230509502:key/83e16ab6-e2c5-4a75-be69-f0af42d60e8b"
    throughput            = 125
    volume_size           = 20
    volume_type           = "gp3"
  }

  tags = {
    Name = "moru-server"
  }

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_eip" "moru_server" {
  domain   = "vpc"
  instance = aws_instance.moru_server.id

  lifecycle {
    prevent_destroy = true
  }
}
