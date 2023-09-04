$apod =  Convert-Path .\APODesktop\bin\Debug\net7.0\APODesktop.exe
$action = New-ScheduledTaskAction -Execute $apod
$trigger = New-ScheduledTaskTrigger -Daily -At '12:45 PM'
$principal = New-ScheduledTaskPrincipal -UserId (whoami)
$settings = New-ScheduledTaskSettingsSet -RunOnlyIfNetworkAvailable
$task = New-ScheduledTask -Action $action -Principal $principal -Trigger $trigger -Settings $settings
$name = 'APOD-Update Wallpaper daily'
$description = 'from https://github.com/vegerot/APODesktop'
Register-ScheduledTask 'SetApodWallpaper' -InputObject $task
