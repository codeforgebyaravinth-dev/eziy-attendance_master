using System;
using System.Threading.Tasks;
using EziyAttendanceMaster.Models;

namespace EziyAttendanceMaster.Services;

public interface IEziyMqttService
{
    event Action<MqttAlertMessage>? OnAlertReceived;
    bool IsConnected { get; }
    Task StartAsync();
    Task StopAsync();
}

