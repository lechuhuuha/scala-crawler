.PHONY: up down logs ps test-control-plane

up:
	docker compose up --build

down:
	docker compose down -v

logs:
	docker compose logs -f

ps:
	docker compose ps

test-control-plane:
	docker compose run --rm control-plane sbt test
