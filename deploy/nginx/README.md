# Nginx privacy logging

These files target the production host running Ubuntu 24.04 and Nginx.

## Access log

Back up `/etc/nginx/nginx.conf`, then replace the existing global
`access_log /var/log/nginx/access.log;` directive with the contents of
`moru-access-log.conf`. Keep the directives inside the `http` block and before
the virtual host includes.

The format records only the timestamp, application response request ID, HTTP
status, and request duration. It intentionally excludes client IPs, request
paths and query strings, headers, request bodies, referrers, and user agents.

## Rotation

`logrotate-moru-nginx` is the version-controlled copy of
`/etc/logrotate.d/nginx`. It rotates logs daily, retains 14 rotations, and
compresses old files. The production host already has an equivalent Ubuntu
configuration, so replace it only when the server configuration differs.

## Verification

```bash
sudo nginx -t
sudo systemctl reload nginx
sudo logrotate -d /etc/logrotate.d/nginx
systemctl status logrotate.timer --no-pager
```

After the application with `X-Request-ID` response support is deployed, make a
health request and verify the access log:

```bash
curl -i -H "X-Request-ID: nginx-privacy-test" https://moru-api.duckdns.org/health
sudo tail -n 5 /var/log/nginx/access.log
```
