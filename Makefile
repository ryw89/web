all: app

app:
	cd deploy/js/ && ansible-playbook  --ask-become-pass update-js.yaml
	cd deploy/jar/ && ansible-playbook --ask-become-pass update-jar.yaml
	cd deploy/service/ && ansible-playbook --ask-become-pass restart-service.yaml
