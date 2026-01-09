$apod =  Convert-Path .\APODesktop\bin\Debug\net7.0\APODesktop.exe
$action = New-ScheduledTaskAction -Execute $apod -Argument "--daemon"
$trigger = New-ScheduledTaskTrigger -Daily -At '12:00 PM'
$principal = New-ScheduledTaskPrincipal -UserId (whoami)
$settings = New-ScheduledTaskSettingsSet -RunOnlyIfNetworkAvailable -StartWhenAvailable -AllowStartIfOnBatteries
$task = New-ScheduledTask -Action $action -Principal $principal -Trigger $trigger -Settings $settings
$name = 'APOD-Update Wallpaper daily'
$description = 'from https://github.com/vegerot/APODesktop'

Register-ScheduledTask $name -InputObject $task
