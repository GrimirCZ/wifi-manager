package grpcclient

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"fmt"
	"log"
	"os"
	"time"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/config"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/routeragentgrpc"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/routeragentpb"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
)

type CommandHandler interface {
	HandleCommand(ctx context.Context, stream *routeragentgrpc.Stream, cmd *routeragentpb.RouterAgentCommand) error
}

type streamLifecycleHandler interface {
	OnStreamConnected(sender *routeragentgrpc.Stream)
	OnStreamDisconnected(sender *routeragentgrpc.Stream)
}

func Run(ctx context.Context, cfg config.Config, handler CommandHandler) error {
	dialOpts, err := buildDialOptions(cfg)
	if err != nil {
		return err
	}

	for {
		select {
		case <-ctx.Done():
			return nil
		default:
		}

		if err := runOnce(ctx, cfg, handler, dialOpts); err != nil {
			log.Printf("grpc connection error: %v", err)
		}

		select {
		case <-ctx.Done():
			return nil
		case <-time.After(cfg.ReconnectDelay):
		}
	}
}

func runOnce(ctx context.Context, cfg config.Config, handler CommandHandler, dialOpts []grpc.DialOption) error {
	client, err := routeragentgrpc.Dial(ctx, cfg.GrpcTarget, dialOpts...)
	if err != nil {
		return err
	}
	defer client.Close()

	streamCtx, cancel := context.WithCancel(ctx)
	defer cancel()

	stream, err := client.Connect(streamCtx)
	if err != nil {
		return err
	}
	if lifecycleHandler, ok := handler.(streamLifecycleHandler); ok {
		lifecycleHandler.OnStreamConnected(stream)
		defer lifecycleHandler.OnStreamDisconnected(stream)
	}

	closeDone := make(chan struct{})
	go func() {
		select {
		case <-streamCtx.Done():
			_ = stream.CloseSend()
		case <-closeDone:
		}
	}()
	defer close(closeDone)

	if err := stream.SendSynchronize("startup"); err != nil {
		return err
	}

	go func() {
		ticker := time.NewTicker(cfg.SyncInterval)
		defer ticker.Stop()
		for {
			select {
			case <-streamCtx.Done():
				return
			case <-ticker.C:
				if err := stream.SendSynchronize("periodic"); err != nil {
					if streamCtx.Err() == nil {
						log.Printf("failed to send periodic synchronize: %v", err)
						cancel()
					}
					return
				}
			}
		}
	}()

	for {
		select {
		case <-streamCtx.Done():
			_ = stream.CloseSend()
			return nil
		default:
		}

		cmd, err := stream.Recv()
		if err != nil {
			if streamCtx.Err() != nil {
				return nil
			}
			return err
		}
		if err := handler.HandleCommand(ctx, stream, cmd); err != nil {
			log.Printf("command handling error: %v", err)
		}
	}
}

func buildDialOptions(cfg config.Config) ([]grpc.DialOption, error) {
	if !cfg.TLSEnabled {
		return nil, nil
	}

	tlsConfig, err := loadTLSConfig(cfg)
	if err != nil {
		return nil, err
	}

	return []grpc.DialOption{
		grpc.WithTransportCredentials(credentials.NewTLS(tlsConfig)),
	}, nil
}

func loadTLSConfig(cfg config.Config) (*tls.Config, error) {
	cert, err := tls.LoadX509KeyPair(cfg.TLSCertFile, cfg.TLSKeyFile)
	if err != nil {
		return nil, fmt.Errorf("load client certificate: %w", err)
	}

	caBytes, err := os.ReadFile(cfg.TLSCAFile)
	if err != nil {
		return nil, fmt.Errorf("read ca file: %w", err)
	}
	caPool := x509.NewCertPool()
	if !caPool.AppendCertsFromPEM(caBytes) {
		return nil, fmt.Errorf("invalid ca certificate")
	}

	tlsConfig := &tls.Config{
		Certificates: []tls.Certificate{cert},
		RootCAs:      caPool,
		MinVersion:   tls.VersionTLS13,
	}
	if cfg.TLSServerName != "" {
		tlsConfig.ServerName = cfg.TLSServerName
	}

	return tlsConfig, nil
}
