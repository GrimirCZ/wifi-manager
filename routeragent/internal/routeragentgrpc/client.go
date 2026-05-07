package routeragentgrpc

import (
	"context"
	"sync"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/routeragentpb"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
)

// Client wraps the RouterAgentService client with convenience helpers.
type Client struct {
	conn    *grpc.ClientConn
	service routeragentpb.RouterAgentServiceClient
}

// Dial connects to a RouterAgentService gRPC server.
// If no dial options are provided, an insecure connection is used.
func Dial(ctx context.Context, target string, opts ...grpc.DialOption) (*Client, error) {
	if len(opts) == 0 {
		opts = []grpc.DialOption{grpc.WithTransportCredentials(insecure.NewCredentials())}
	}

	conn, err := grpc.DialContext(ctx, target, opts...)
	if err != nil {
		return nil, err
	}

	return &Client{
		conn:    conn,
		service: routeragentpb.NewRouterAgentServiceClient(conn),
	}, nil
}

// Close shuts down the underlying gRPC connection.
func (c *Client) Close() error {
	return c.conn.Close()
}

// Connect opens the bidirectional stream with the router agent service.
func (c *Client) Connect(ctx context.Context, opts ...grpc.CallOption) (*Stream, error) {
	stream, err := c.service.Connect(ctx, opts...)
	if err != nil {
		return nil, err
	}

	return &Stream{stream: stream}, nil
}

// Stream wraps the bidirectional stream for sending agent messages and receiving commands.
type Stream struct {
	stream routeragentpb.RouterAgentService_ConnectClient
	sendMu sync.Mutex
}

// Recv waits for the next command from the server.
func (s *Stream) Recv() (*routeragentpb.RouterAgentCommand, error) {
	return s.stream.Recv()
}

// SendSynchronize publishes a Synchronize message on the stream.
func (s *Stream) SendSynchronize(reason string) error {
	s.sendMu.Lock()
	defer s.sendMu.Unlock()
	return s.stream.Send(&routeragentpb.RouterAgentMessage{
		Message: &routeragentpb.RouterAgentMessage_Synchronize{
			Synchronize: &routeragentpb.Synchronize{Reason: reason},
		},
	})
}

// SendAck publishes a CommandAck message on the stream.
func (s *Stream) SendAck(ack *routeragentpb.CommandAck) error {
	s.sendMu.Lock()
	defer s.sendMu.Unlock()
	return s.stream.Send(&routeragentpb.RouterAgentMessage{
		Message: &routeragentpb.RouterAgentMessage_Ack{Ack: ack},
	})
}

func (s *Stream) SendAuthorizedClientObserved(observed *routeragentpb.AuthorizedClientObserved) error {
	s.sendMu.Lock()
	defer s.sendMu.Unlock()
	return s.stream.Send(&routeragentpb.RouterAgentMessage{
		Message: &routeragentpb.RouterAgentMessage_AuthorizedClientObserved{
			AuthorizedClientObserved: observed,
		},
	})
}

// SendAllowedClientsPresence publishes periodic presence of allowed MACs and their last observed timestamps.
func (s *Stream) SendAllowedClientsPresence(presence *routeragentpb.AllowedClientsPresence) error {
	s.sendMu.Lock()
	defer s.sendMu.Unlock()
	return s.stream.Send(&routeragentpb.RouterAgentMessage{
		Message: &routeragentpb.RouterAgentMessage_AllowedClientsPresence{
			AllowedClientsPresence: presence,
		},
	})
}

// CloseSend closes the send direction of the stream.
func (s *Stream) CloseSend() error {
	return s.stream.CloseSend()
}
