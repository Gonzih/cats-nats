cs-install:
	curl -fL https://github.com/coursier/launchers/raw/master/cs-x86_64-pc-linux.gz | gzip -d > /tmp/cs
	sudo cp -f /tmp/cs /usr/bin/cs
	sudo chmod +x /usr/bin/cs

cs-setup:
	cs setup --yes

ci:
	env NATS_URL="nats://localhost:4222" sbt test
