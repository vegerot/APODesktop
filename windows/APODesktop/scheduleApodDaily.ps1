$ErrorActionPreference = 'Stop'
$installDirectory = Join-Path $env:LOCALAPPDATA 'Programs\APODesktop'
dotnet publish "$PSScriptRoot\APODesktop\APODesktop.csproj" --configuration Release --output $installDirectory
if ($LASTEXITCODE -ne 0) {
    throw 'APODesktop publish failed.'
}
$apod = Join-Path $installDirectory 'APODesktop.exe'
$action = New-ScheduledTaskAction -Execute $apod
$trigger = New-ScheduledTaskTrigger -Daily -At '12:00 PM'
$principal = New-ScheduledTaskPrincipal -UserId (whoami)
$settings = New-ScheduledTaskSettingsSet -RunOnlyIfNetworkAvailable -StartWhenAvailable -AllowStartIfOnBatteries
$task = New-ScheduledTask -Action $action -Principal $principal -Trigger $trigger -Settings $settings
$name = 'APOD-Update Wallpaper daily'
$description = 'from https://github.com/vegerot/APODesktop'

Register-ScheduledTask $name -InputObject $task -Force
