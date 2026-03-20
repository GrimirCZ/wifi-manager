.PHONY: proto-gen dummy-router build build-linux-amd64

dummy-router:
	ROUTERAGENT_GRPC_TARGET='localhost:9091' \
	ROUTERAGENT_DUMMY_MODE=true \
	go run main.go

build:
	go build

build-linux-amd64:
	GOOS=linux GOARCH=amd64 go build

proto-gen:
	buf generate ../captive/routeragent/src/main/proto/router_agent.proto \
		--template ./buf.gen.yaml \
		-o .
