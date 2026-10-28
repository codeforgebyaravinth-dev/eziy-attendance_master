using System;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using EziyAttendanceMaster.Models;
using MQTTnet;
using MQTTnet.Client;

namespace EziyAttendanceMaster.Services;

public class EziyMqttService : IEziyMqttService, IDisposable
{
    private readonly ISecureStorageService _prefs;
    private IMqttClient? _mqttClient;
    private MqttClientOptions? _options;
    private CancellationTokenSource? _cts;

    public event Action<MqttAlertMessage>? OnAlertReceived;

    public bool IsConnected => _mqttClient?.IsConnected ?? false;

    public EziyMqttService(ISecureStorageService prefs)
    {
        _prefs = prefs;
    }

    public async Task StartAsync()
    {
        var badge = _prefs.EmployeeId;
        if (badge.Length != 8) return;

        try
        {
            await StopAsync();

            _cts = new CancellationTokenSource();
            var factory = new MqttFactory();
            _mqttClient = factory.CreateMqttClient();

            var clientId = $"eziy-win-{badge}-{Guid.NewGuid().ToString("N")[..8]}";

            _options = new MqttClientOptionsBuilder()
                .WithTcpServer("mqtt.eziy.in", 1883)
                .WithCredentials("atc", "ahxpp6467c")
                .WithClientId(clientId)
                .WithCleanSession()
                .WithTimeout(TimeSpan.FromSeconds(15))
                .Build();

            _mqttClient.ApplicationMessageReceivedAsync += e =>
            {
                try
                {
                    var raw = Encoding.UTF8.GetString(e.ApplicationMessage.PayloadSegment);
                    System.Diagnostics.Debug.WriteLine($"MQTT Message on {e.ApplicationMessage.Topic}: {raw}");

                    var alert = JsonSerializer.Deserialize<MqttAlertMessage>(raw);
                    if (alert != null)
                    {
                        OnAlertReceived?.Invoke(alert);
                    }
                    else
                    {
                        OnAlertReceived?.Invoke(new MqttAlertMessage
                        {
                            EmployeeName = badge,
                            Status = raw,
                            Timestamp = DateTime.UtcNow.ToString("o")
                        });
                    }
                }
                catch (Exception ex)
                {
                    System.Diagnostics.Debug.WriteLine($"MQTT Parse Error: {ex.Message}");
                }
                return Task.CompletedTask;
            };

            _mqttClient.ConnectedAsync += async e =>
            {
                System.Diagnostics.Debug.WriteLine($"MQTT Connected. Subscribing to topic: {badge}");
                var subscribeOptions = factory.CreateMqttClientSubscribeOptionsBuilder()
                    .WithTopicFilter(f => f.WithTopic(badge).WithAtLeastOnceQoS())
                    .Build();
                await _mqttClient.SubscribeAsync(subscribeOptions, CancellationToken.None);
            };

            await _mqttClient.ConnectAsync(_options, _cts.Token);
        }
        catch (Exception ex)
        {
            System.Diagnostics.Debug.WriteLine($"MQTT Connection Failed: {ex.Message}");
        }
    }

    public async Task StopAsync()
    {
        try
        {
            _cts?.Cancel();
            if (_mqttClient != null && _mqttClient.IsConnected)
            {
                await _mqttClient.DisconnectAsync();
            }
        }
        catch { }
        finally
        {
            _mqttClient?.Dispose();
            _mqttClient = null;
        }
    }

    public void Dispose()
    {
        _ = StopAsync();
    }
}

