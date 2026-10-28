using System;
using System.Runtime.InteropServices;
using System.Windows.Interop;

namespace EziyAttendanceMaster.Linphone;

public class LinphoneVideoHost : HwndHost
{
    private const int WsChild = 0x40000000;
    private const int WsVisible = 0x10000000;
    private const int WsClipsiblings = 0x04000000;
    private const int WsClipchildren = 0x02000000;

    public IntPtr Hwnd { get; private set; } = IntPtr.Zero;

    protected override HandleRef BuildWindowCore(HandleRef hwndParent)
    {
        Hwnd = CreateWindowEx(
            0,
            "static",
            "",
            WsChild | WsVisible | WsClipsiblings | WsClipchildren,
            0, 0,
            (int)ActualWidth, (int)ActualHeight,
            hwndParent.Handle,
            IntPtr.Zero,
            IntPtr.Zero,
            0);

        return new HandleRef(this, Hwnd);
    }

    protected override void DestroyWindowCore(HandleRef hwnd)
    {
        DestroyWindow(hwnd.Handle);
        Hwnd = IntPtr.Zero;
    }

    [DllImport("user32.dll", SetLastError = true, EntryPoint = "CreateWindowExW")]
    private static extern IntPtr CreateWindowEx(
        int dwExStyle,
        [MarshalAs(UnmanagedType.LPWStr)] string lpszClassName,
        [MarshalAs(UnmanagedType.LPWStr)] string lpszWindowName,
        int style,
        int x, int y,
        int width, int height,
        IntPtr hwndParent,
        IntPtr hMenu,
        IntPtr hInst,
        [MarshalAs(UnmanagedType.AsAny)] object pvParam);

    [DllImport("user32.dll", EntryPoint = "DestroyWindow", CharSet = CharSet.Unicode)]
    private static extern bool DestroyWindow(IntPtr hwnd);
}

