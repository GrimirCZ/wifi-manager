.PHONY: proto-gen dummy-router

dummy-router:
	ROUTERAGENT_GRPC_TARGET='localhost:9091' \
	ROUTERAGENT_DUMMY_MODE=true \
	go run main.go

proto-gen:
	buf generate ../captive/routeragent/src/main/proto/router_agent.proto \
		--template ./buf.gen.yaml \
		-o .
