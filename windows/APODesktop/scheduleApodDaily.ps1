$apod =  Convert-Path .\APODesktop\bin\Debug\net10.0-windows\APODesktop.exe
$action = New-ScheduledTaskAction -Execute $apod
$trigger = New-ScheduledTaskTrigger -Daily -At '12:00 PM'
$principal = New-ScheduledTaskPrincipal -UserId (whoami)
$settings = New-ScheduledTaskSettingsSet -RunOnlyIfNetworkAvailable -StartWhenAvailable -AllowStartIfOnBatteries
$task = New-ScheduledTask -Action $action -Principal $principal -Trigger $trigger -Settings $settings
$name = 'APOD-Update Wallpaper daily'
$description = 'from https://github.com/vegerot/APODesktop'

if (Get-ScheduledTask -TaskName $name -ErrorAction SilentlyContinue) {
    Unregister-ScheduledTask -TaskName $name -Confirm:$false
}

Register-ScheduledTask $name -InputObject $task
