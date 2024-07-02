package com.miui.base;

import com.android.server.AppOpsServiceStubImpl;
import com.android.server.BootKeeperStubImpl;
import com.android.server.ConsumerIrServiceStubImpl;
import com.android.server.ExtendMImpl;
import com.android.server.MiuiBatteryServiceImpl;
import com.android.server.MiuiCldImpl;
import com.android.server.MiuiFallbackHelper;
import com.android.server.MiuiUiModeManagerServiceStubImpl;
import com.android.server.MountServiceIdlerImpl;
import com.android.server.PackageWatchdogImpl;
import com.android.server.PinnerServiceImpl;
import com.android.server.ProcHunterImpl;
import com.android.server.RescuePartyImpl;
import com.android.server.ScoutSystemMonitor;
import com.android.server.StorageManagerServiceImpl;
import com.android.server.SystemServerImpl;
import com.android.server.WatchdogImpl;
import com.android.server.WiredAccessoryManagerStubImpl;
import com.android.server.accessibility.AccessibilityStateListenerImpl;
import com.android.server.accounts.AccountManagerServiceImpl;
import com.android.server.alarm.AlarmManagerServiceStubImpl;
import com.android.server.am.ActiveServiceManagementImpl;
import com.android.server.am.ActivityManagerServiceImpl;
import com.android.server.am.AppErrorDialogImpl;
import com.android.server.am.AppErrorsImpl;
import com.android.server.am.AppNotRespondingDialogImpl;
import com.android.server.am.AppProfilerImpl;
import com.android.server.am.AutoStartManagerServiceStubImpl;
import com.android.server.am.BinderProxyMonitorImpl;
import com.android.server.am.BroadcastQueueModernStubImpl;
import com.android.server.am.ContentProviderRecordImpl;
import com.android.server.am.MemoryFreezeStubImpl;
import com.android.server.am.MemoryStandardProcessControl;
import com.android.server.am.MimdManagerServiceImpl;
import com.android.server.am.MiuiBoosterUtilsStubImpl;
import com.android.server.am.MiuiMemoryInfoImpl;
import com.android.server.am.MiuiProcessStubImpl;
import com.android.server.am.MutableActiveServicesStubImpl;
import com.android.server.am.MutableActivityManagerShellCommandStubImpl;
import com.android.server.am.OomAdjusterImpl;
import com.android.server.am.PendingIntentControllerStubImpl;
import com.android.server.am.PendingIntentRecordImpl;
import com.android.server.am.PreloadAppControllerImpl;
import com.android.server.am.ProcessListStubImpl;
import com.android.server.am.ProcessProphetImpl;
import com.android.server.am.ProcessRecordImpl;
import com.android.server.am.SchedBoostManagerInternalStubImpl;
import com.android.server.am.SmartPowerService;
import com.android.server.am.SystemPressureController;
import com.android.server.app.GameManagerServiceStubImpl;
import com.android.server.app.PreCacheUtilStubImpl;
import com.android.server.appcacheopt.AppCacheOptimizer;
import com.android.server.appwidget.MiuiAppWidgetServiceImpl;
import com.android.server.audio.AudioDeviceBrokerStubImpl;
import com.android.server.audio.AudioDeviceInventoryStubImpl;
import com.android.server.audio.AudioServiceStubImpl;
import com.android.server.audio.BtHelperImpl;
import com.android.server.audio.FocusRequesterStubImpl;
import com.android.server.audio.MediaFocusControlStubImpl;
import com.android.server.audio.PlaybackActivityMonitorStubImpl;
import com.android.server.audio.SoundDoseHelperStubImpl;
import com.android.server.audio.SpatializerHelperStubImpl;
import com.android.server.autofill.ui.SaveUiStubImpl;
import com.android.server.backup.BackupManagerServiceInjector;
import com.android.server.backup.SystemBackupAgentImpl;
import com.android.server.biometrics.sensors.face.FaceServiceStubImpl;
import com.android.server.biometrics.sensors.fingerprint.FingerprintServiceInjectorStubImpl;
import com.android.server.biometrics.sensors.fingerprint.FingerprintServiceStubImpl;
import com.android.server.biometrics.sensors.fingerprint.FodFingerprintServiceStubImpl;
import com.android.server.biometrics.sensors.fingerprint.HeartRateFingerprintServiceStubImpl;
import com.android.server.biometrics.sensors.fingerprint.hidl.PowerFingerprintServiceStubImpl;
import com.android.server.camera.CameraActivitySceneMode;
import com.android.server.camera.CameraOpt;
import com.android.server.clipboard.ClipboardServiceStubImpl;
import com.android.server.cloud.MiuiCommonCloudHelper;
import com.android.server.cloud.MiuiCommonCloudService;
import com.android.server.content.ContentServiceStubImpl;
import com.android.server.content.SyncManagerStubImpl;
import com.android.server.content.SyncSecurityStubImpl;
import com.android.server.content.SyncStorageEngineStubImpl;
import com.android.server.devicepolicy.DevicePolicyManagerServiceStubImpl;
import com.android.server.devicestate.DeviceStateManagerServiceImpl;
import com.android.server.display.AutomaticBrightnessControllerImpl;
import com.android.server.display.BrightnessMappingStrategyImpl;
import com.android.server.display.DisplayAdapterImpl;
import com.android.server.display.DisplayFeatureManagerServiceImpl;
import com.android.server.display.DisplayGroupImpl;
import com.android.server.display.DisplayManagerServiceImpl;
import com.android.server.display.DisplayPowerControllerImpl;
import com.android.server.display.HysteresisLevelsImpl;
import com.android.server.display.LocalDisplayAdapterImpl;
import com.android.server.display.MiuiBrightnessUtilsImpl;
import com.android.server.display.mode.DisplayModeDirectorImpl;
import com.android.server.input.InputManagerServiceStubImpl;
import com.android.server.input.InputShellCommandStubImpl;
import com.android.server.input.KeyboardCombinationManagerStubImpl;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import com.android.server.inputmethod.MiuiInputMethodStubImpl;
import com.android.server.job.JobServiceContextImpl;
import com.android.server.lights.LightsManagerImpl;
import com.android.server.location.GnssCollectDataImpl;
import com.android.server.location.GnssMockLocationOptImpl;
import com.android.server.location.GnssSmartSatelliteSwitchImpl;
import com.android.server.location.LocationDumpLogImpl;
import com.android.server.location.LocationExtCooperateImpl;
import com.android.server.location.MiuiBlurLocationManagerImpl;
import com.android.server.location.MtkGnssPowerSaveImpl;
import com.android.server.location.gnss.GnssConfigurationImpl;
import com.android.server.location.gnss.GnssLocationProviderImpl;
import com.android.server.location.gnss.datacollect.GnssEventTrackingImpl;
import com.android.server.location.gnss.exp.GnssBackgroundUsageOptImpl;
import com.android.server.location.gnss.gnssSelfRecovery.GnssSelfRecovery;
import com.android.server.location.gnss.hal.GnssNativeImpl;
import com.android.server.location.gnss.hal.GnssPowerOptimizeImpl;
import com.android.server.location.gnss.hal.GnssScoringModelImpl;
import com.android.server.location.gnss.monitor.LocationSettingsEventMonitorImpl;
import com.android.server.location.gnss.operators.GnssForCommonOperatorImpl;
import com.android.server.location.gnss.operators.GnssForKtCustomImpl;
import com.android.server.location.gnss.operators.GnssForTelcelCustomImpl;
import com.android.server.location.provider.AmapCustomImpl;
import com.android.server.location.provider.LocationProviderManagerImpl;
import com.android.server.locksettings.LockSettingsImpl;
import com.android.server.logcat.LogcatManagerServiceImpl;
import com.android.server.media.MediaSessionServiceStubImpl;
import com.android.server.media.projection.MediaProjectionManagerServiceStubImpl;
import com.android.server.miuibpf.MiuiBpfServiceImpl;
import com.android.server.net.NetworkManagermentServiceImpl;
import com.android.server.net.NetworkPolicyManagerServiceImpl;
import com.android.server.net.NetworkTimeUpdateServiceImpl;
import com.android.server.notification.BarrageListenerServiceImpl;
import com.android.server.notification.NotificationManagerServiceImpl;
import com.android.server.notification.ZenModeStubImpl;
import com.android.server.pm.Miui32BitTranslateHelper;
import com.android.server.pm.MiuiDefaultPermissionGrantPolicy;
import com.android.server.pm.MiuiPreinstallCompressImpl;
import com.android.server.pm.MiuiPreinstallHelper;
import com.android.server.pm.PackageManagerServiceImpl;
import com.android.server.pm.PackageManagerServiceUtilsImpl;
import com.android.server.pm.PerfTimeMonitorImpl;
import com.android.server.pm.SettingsImpl;
import com.android.server.pm.UserManagerServiceImpl;
import com.android.server.pm.permission.MiPermissionManagerServiceImpl;
import com.android.server.policy.PhoneWindowManagerStubImpl;
import com.android.server.power.PowerManagerServiceImpl;
import com.android.server.power.ShutdownThreadImpl;
import com.android.server.power.stats.BatteryStatsManagerStubImpl;
import com.android.server.power.stats.ScreenPowerCalculatorImpl;
import com.android.server.print.MiuiPrintManager;
import com.android.server.recoverysystem.RecoverySystemServiceImpl;
import com.android.server.sensors.SensorServiceImpl;
import com.android.server.statusbar.StatusBarManagerServiceStubImpl;
import com.android.server.storage.DeviceStorageMonitorServiceImpl;
import com.android.server.testharness.TestHarnessModeServiceImpl;
import com.android.server.uri.UriGrantsManagerServiceImpl;
import com.android.server.usage.UsageStatsServiceImpl;
import com.android.server.usb.MiuiUsbServiceImpl;
import com.android.server.vibrator.VibratorManagerServiceImpl;
import com.android.server.wallpaper.WallpaperManagerServiceImpl;
import com.android.server.wm.ActivityRecordImpl;
import com.android.server.wm.ActivityStarterImpl;
import com.android.server.wm.ActivityTaskManagerServiceImpl;
import com.android.server.wm.ActivityTaskSupervisorImpl;
import com.android.server.wm.AppRTWmsImpl;
import com.android.server.wm.AppResurrectionServiceImpl;
import com.android.server.wm.AsyncRotationControllerImpl;
import com.android.server.wm.BoundsCompatController;
import com.android.server.wm.DimmerImpl;
import com.android.server.wm.DisplayContentStubImpl;
import com.android.server.wm.DisplayPolicyStubImpl;
import com.android.server.wm.DisplayRotationStubImpl;
import com.android.server.wm.DragDropControllerImpl;
import com.android.server.wm.FindDeviceLockWindowImpl;
import com.android.server.wm.ImmersiveModeConfirmationImpl;
import com.android.server.wm.InsetsPolicyStubImpl;
import com.android.server.wm.LaunchParamsControllerImpl;
import com.android.server.wm.LetterboxImpl;
import com.android.server.wm.MIUIWatermark;
import com.android.server.wm.MiGameTypeIdentificationImpl;
import com.android.server.wm.MirrorActiveUidsImpl;
import com.android.server.wm.MiuiContrastOverlayStubImpl;
import com.android.server.wm.MiuiDisplayReportImpl;
import com.android.server.wm.MiuiDragAndDropStubImpl;
import com.android.server.wm.MiuiDragStateImpl;
import com.android.server.wm.MiuiFreeFormManagerService;
import com.android.server.wm.MiuiFreeformUtilImpl;
import com.android.server.wm.MiuiFreezeImpl;
import com.android.server.wm.MiuiMiPerfStubImpl;
import com.android.server.wm.MiuiMirrorDragEventImpl;
import com.android.server.wm.MiuiMultiTaskManager;
import com.android.server.wm.MiuiMultiWindowServiceImpl;
import com.android.server.wm.MiuiOrientationImpl;
import com.android.server.wm.MiuiPaperContrastOverlayStubImpl;
import com.android.server.wm.MiuiRefreshRatePolicy;
import com.android.server.wm.MiuiScreenProjectionServiceExStubImpl;
import com.android.server.wm.MiuiSplitInputMethodImpl;
import com.android.server.wm.MiuiWindowMonitorImpl;
import com.android.server.wm.MultiSenceManagerInternalStubImpl;
import com.android.server.wm.OpenBrowserWithUrlImpl;
import com.android.server.wm.OrientationSensorJudgeImpl;
import com.android.server.wm.PreloadStateManagerImpl;
import com.android.server.wm.ProcessCompatController;
import com.android.server.wm.RealTimeModeControllerImpl;
import com.android.server.wm.ScreenRotationAnimationImpl;
import com.android.server.wm.TalkbackWatermark;
import com.android.server.wm.TaskSnapshotControllerInjectorImpl;
import com.android.server.wm.TaskSnapshotPersisterInjectorImpl;
import com.android.server.wm.TaskStubImpl;
import com.android.server.wm.WallpaperControllerImpl;
import com.android.server.wm.WallpaperWindowTokenImpl;
import com.android.server.wm.WindowContainerImpl;
import com.android.server.wm.WindowManagerServiceImpl;
import com.android.server.wm.WindowStateAnimatorImpl;
import com.android.server.wm.WindowStateStubImpl;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.security.SafetyDetectManagerImpl;
import com.miui.server.sptm.SpeedTestModeServiceImpl;
import com.miui.server.stability.LockPerfImpl;
import com.miui.server.xspace.XSpaceManagerServiceImpl;
import com.xiaomi.mirror.MirrorServiceImpl;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
public class ServiceManifestCollector {
    public static void collectManifests(List list) {
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.ConsumerIrServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.ConsumerIrServiceStub", new ConsumerIrServiceStubImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.ExtendMStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.ExtendMStub", new ExtendMImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.MiuiBatteryServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.MiuiBatteryServiceStub", new MiuiBatteryServiceImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.MiuiCldStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.MiuiCldStub", new MiuiCldImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.MiuiCommonCloudServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.MiuiCommonCloudHelperStub", new MiuiCommonCloudHelper.Provider());
                outProviders.put("com.android.server.MiuiCommonCloudServiceStub", new MiuiCommonCloudService.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.MiuiPaperContrastOverlayStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.wm.MiuiPaperContrastOverlayStub", new MiuiPaperContrastOverlayStubImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.MiuiStubImplManifest$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.uri.UriGrantsManagerServiceStub", new UriGrantsManagerServiceImpl.Provider());
                outProviders.put("com.android.server.SystemServerStub", new SystemServerImpl.Provider());
                outProviders.put("com.android.server.PinnerServiceStub", new PinnerServiceImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.MiuiUiModeManagerServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.MiuiUiModeManagerServiceStub", new MiuiUiModeManagerServiceStubImpl.Provider());
                outProviders.put("com.android.server.wm.MiuiContrastOverlayStub", new MiuiContrastOverlayStubImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.NetworkManagementServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.net.NetworkPolicyManagerServiceStub", new NetworkPolicyManagerServiceImpl.Provider());
                outProviders.put("com.android.server.net.NetworkManagementServiceStub", new NetworkManagermentServiceImpl.Provider());
                outProviders.put("com.android.server.net.NetworkTimeUpdateServiceStub", new NetworkTimeUpdateServiceImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.WatchdogStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.ScoutStub", new ScoutSystemMonitor.Provider());
                outProviders.put("com.android.server.ProcHunterStub", new ProcHunterImpl.Provider());
                outProviders.put("com.android.server.RescuePartyStub", new RescuePartyImpl.Provider());
                outProviders.put("com.android.server.BootKeeperStub", new BootKeeperStubImpl.Provider());
                outProviders.put("com.android.server.am.BinderProxyMonitor", new BinderProxyMonitorImpl.Provider());
                outProviders.put("com.android.server.PackageWatchdogStub", new PackageWatchdogImpl.Provider());
                outProviders.put("com.android.server.WatchdogStub", new WatchdogImpl.Provider());
                outProviders.put("com.android.server.LockPerfStub", new LockPerfImpl.Provider());
                outProviders.put("com.android.server.MiuiFallbackHelperStub", new MiuiFallbackHelper.Provider());
                outProviders.put("com.android.server.recoverysystem.RecoverySystemServiceStub", new RecoverySystemServiceImpl.Provider());
                outProviders.put("com.android.server.am.AppProfilerStub", new AppProfilerImpl.Provider());
                outProviders.put("com.android.server.wm.MiuiWindowMonitorStub", new MiuiWindowMonitorImpl.Provider());
                outProviders.put("com.android.server.miuibpf.MiuiBpfServiceStub", new MiuiBpfServiceImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.accessibility.AccessibilityStateListenerStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.accessibility.AccessibilityStateListenerStub", new AccessibilityStateListenerImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.accounts.AccountManagerServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.accounts.AccountManagerServiceStub", new AccountManagerServiceImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.alarm.AlarmManagerServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.alarm.AlarmManagerServiceStub", new AlarmManagerServiceStubImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.am.ActivityManagerServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.am.AppNotRespondingDialogStub", new AppNotRespondingDialogImpl.Provider());
                outProviders.put("com.android.server.am.ProcessRecordStub", new ProcessRecordImpl.Provider());
                outProviders.put("com.android.server.wm.WindowStateAnimatorStub", new WindowStateAnimatorImpl.Provider());
                outProviders.put("com.android.server.wm.ImmersiveModeConfirmationStub", new ImmersiveModeConfirmationImpl.Provider());
                outProviders.put("com.android.server.wm.TalkbackWatermarkStub", new TalkbackWatermark.Provider());
                outProviders.put("com.android.server.wm.DimmerStub", new DimmerImpl.Provider());
                outProviders.put("com.android.server.wm.OpenBrowserWithUrlStub", new OpenBrowserWithUrlImpl.Provider());
                outProviders.put("com.android.server.wm.ActivityTaskManagerServiceStub", new ActivityTaskManagerServiceImpl.Provider());
                outProviders.put("com.android.server.am.ActivityManagerServiceStub", new ActivityManagerServiceImpl.Provider());
                outProviders.put("com.android.server.policy.PhoneWindowManagerStub", new PhoneWindowManagerStubImpl.Provider());
                outProviders.put("com.android.server.am.ActiveServiceManagementStub", new ActiveServiceManagementImpl.Provider());
                outProviders.put("com.android.server.am.PendingIntentRecordStub", new PendingIntentRecordImpl.Provider());
                outProviders.put("com.android.server.am.AppErrorsStub", new AppErrorsImpl.Provider());
                outProviders.put("com.android.server.input.InputShellCommandStub", new InputShellCommandStubImpl.Provider());
                outProviders.put("com.android.server.wm.DisplayPolicyStub", new DisplayPolicyStubImpl.Provider());
                outProviders.put("com.android.server.wm.LetterboxStub", new LetterboxImpl.Provider());
                outProviders.put("com.android.server.wm.MirrorActiveUidsStub", new MirrorActiveUidsImpl.Provider());
                outProviders.put("com.android.server.am.PendingIntentControllerStub", new PendingIntentControllerStubImpl.Provider());
                outProviders.put("com.android.server.wm.IMiuiScreenProjectionServiceExStub", new MiuiScreenProjectionServiceExStubImpl.Provider());
                outProviders.put("com.android.server.wm.MiuiFreeformUtilStub", new MiuiFreeformUtilImpl.Provider());
                outProviders.put("com.android.server.input.KeyboardCombinationManagerStub", new KeyboardCombinationManagerStubImpl.Provider());
                outProviders.put("com.android.server.wm.TaskSnapshotControllerInjectorStub", new TaskSnapshotControllerInjectorImpl.Provider());
                outProviders.put("com.android.server.wm.MiuiMiPerfStub", new MiuiMiPerfStubImpl.Provider());
                outProviders.put("com.android.server.wm.MiuiDragAndDropStub", new MiuiDragAndDropStubImpl.Provider());
                outProviders.put("com.android.server.wm.ActivityRecordStub", new ActivityRecordImpl.Provider());
                outProviders.put("com.android.server.am.AutoStartManagerServiceStub", new AutoStartManagerServiceStubImpl.Provider());
                outProviders.put("com.android.server.wm.MiuiMultiWindowServiceStub", new MiuiMultiWindowServiceImpl.Provider());
                outProviders.put("com.android.server.wm.RefreshRatePolicyStub", new MiuiRefreshRatePolicy.Provider());
                outProviders.put("com.android.server.wm.MiuiOrientationStub", new MiuiOrientationImpl.Provider());
                outProviders.put("com.android.server.wm.FindDeviceLockWindowStub", new FindDeviceLockWindowImpl.Provider());
                outProviders.put("com.android.server.wm.DragStateStub", new MiuiDragStateImpl.Provider());
                outProviders.put("com.android.server.wm.AsyncRotationControllerStub", new AsyncRotationControllerImpl.Provider());
                outProviders.put("com.android.server.wm.MiuiFreeFormManagerServiceStub", new MiuiFreeFormManagerService.Provider());
                outProviders.put("com.android.server.wm.TaskSnapshotPersisterInjectorStub", new TaskSnapshotPersisterInjectorImpl.Provider());
                outProviders.put("com.android.server.wm.ScreenRotationAnimationStub", new ScreenRotationAnimationImpl.Provider());
                outProviders.put("com.android.server.am.MutableActivityManagerShellCommandStub", new MutableActivityManagerShellCommandStubImpl.Provider());
                outProviders.put("com.android.server.wm.MiuiMultiTaskManagerStub", new MiuiMultiTaskManager.Provider());
                outProviders.put("com.android.server.wm.MiuiDisplayReportStub", new MiuiDisplayReportImpl.Provider());
                outProviders.put("com.android.server.input.InputManagerServiceStub", new InputManagerServiceStubImpl.Provider());
                outProviders.put("com.android.server.wm.MIUIWatermarkStub", new MIUIWatermark.Provider());
                outProviders.put("com.android.server.wm.LaunchParamsControllerStub", new LaunchParamsControllerImpl.Provider());
                outProviders.put("com.android.server.am.AppErrorDialogStub", new AppErrorDialogImpl.Provider());
                outProviders.put("com.android.server.wm.TaskStub", new TaskStubImpl.Provider());
                outProviders.put("com.android.server.wm.DragDropControllerStub", new DragDropControllerImpl.Provider());
                outProviders.put("com.android.server.wm.DisplayContentStub$MutableDisplayContentStub", new DisplayContentStubImpl.MutableDisplayContentImpl.Provider());
                outProviders.put("com.android.server.wm.OrientationSensorJudgeStub", new OrientationSensorJudgeImpl.Provider());
                outProviders.put("com.android.server.wm.TaskStub$MutableTaskStub", new TaskStubImpl.MutableTaskStubImpl.Provider());
                outProviders.put("com.android.server.wm.WindowManagerServiceStub", new WindowManagerServiceImpl.Provider());
                outProviders.put("com.android.server.wm.WallpaperWindowTokenStub", new WallpaperWindowTokenImpl.Provider());
                outProviders.put("com.android.server.wm.WindowContainerStub", new WindowContainerImpl.Provider());
                outProviders.put("com.android.server.am.ContentProviderRecordStub", new ContentProviderRecordImpl.Provider());
                outProviders.put("com.android.server.wm.WallpaperControllerStub", new WallpaperControllerImpl.Provider());
                outProviders.put("com.android.server.wm.ActivityTaskSupervisorStub", new ActivityTaskSupervisorImpl.Provider());
                outProviders.put("com.android.server.am.MutableActiveServicesStub", new MutableActiveServicesStubImpl.Provider());
                outProviders.put("com.android.server.wm.DisplayContentStub", new DisplayContentStubImpl.Provider());
                outProviders.put("com.android.server.wm.ActivityStarterStub", new ActivityStarterImpl.Provider());
                outProviders.put("com.android.server.wm.ActivityRecordStub$MutableActivityRecordStub", new ActivityRecordImpl.MutableActivityRecordImpl.Provider());
                outProviders.put("com.android.server.wm.WindowStateStub", new WindowStateStubImpl.Provider());
                outProviders.put("com.android.server.am.BroadcastQueueModernStub", new BroadcastQueueModernStubImpl.Provider());
                outProviders.put("com.android.server.wm.InsetsPolicyStub", new InsetsPolicyStubImpl.Provider());
                outProviders.put("com.android.server.wm.AppRTWmsStub", new AppRTWmsImpl.Provider());
                outProviders.put("com.android.server.wm.MiuiMirrorDragEventStub", new MiuiMirrorDragEventImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.am.SystemPressureControllerStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.am.PreloadAppControllerStub", new PreloadAppControllerImpl.Provider());
                outProviders.put("com.android.server.am.ProcessListStub", new ProcessListStubImpl.Provider());
                outProviders.put("com.android.server.am.MiuiProcessStub", new MiuiProcessStubImpl.Provider());
                outProviders.put("com.android.server.am.MiuiBoosterUtilsStub", new MiuiBoosterUtilsStubImpl.Provider());
                outProviders.put("com.android.server.am.MimdManagerServiceStub", new MimdManagerServiceImpl.Provider());
                outProviders.put("com.android.server.am.MemoryStandardProcessControlStub", new MemoryStandardProcessControl.Provider());
                outProviders.put("com.android.server.wm.RealTimeModeControllerStub", new RealTimeModeControllerImpl.Provider());
                outProviders.put("com.android.server.appcacheopt.AppCacheOptimizerStub", new AppCacheOptimizer.Provider());
                outProviders.put("com.android.server.am.OomAdjusterStub", new OomAdjusterImpl.Provider());
                outProviders.put("com.android.server.am.SystemPressureControllerStub", new SystemPressureController.Provider());
                outProviders.put("com.android.server.am.MiuiMemoryInfoStub", new MiuiMemoryInfoImpl.Provider());
                outProviders.put("com.android.server.am.ProcessProphetStub", new ProcessProphetImpl.Provider());
                outProviders.put("com.android.server.am.SchedBoostManagerInternalStub", new SchedBoostManagerInternalStubImpl.Provider());
                outProviders.put("com.android.server.wm.MiuiFreezeStub", new MiuiFreezeImpl.Provider());
                outProviders.put("com.android.server.am.MemoryFreezeStub", new MemoryFreezeStubImpl.Provider());
                outProviders.put("com.android.server.wm.PreloadStateManagerStub", new PreloadStateManagerImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.app.GameManagerServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.wm.MiGameTypeIdentificationStub", new MiGameTypeIdentificationImpl.Provider());
                outProviders.put("com.android.server.app.GameManagerServiceStub", new GameManagerServiceStubImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.app.PreCacheUtilStubImpl$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.app.PreCacheUtilStub", new PreCacheUtilStubImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.appop.AppOpsServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.appop.AppOpsServiceStub", new AppOpsServiceStubImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.appwidget.MiuiAppWidgetServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.appwidget.MiuiAppWidgetServiceStub", new MiuiAppWidgetServiceImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.audio.AudioServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.WiredAccessoryManagerStub", new WiredAccessoryManagerStubImpl.Provider());
                outProviders.put("com.android.server.audio.SpatializerHelperStub", new SpatializerHelperStubImpl.Provider());
                outProviders.put("com.android.server.audio.PlaybackActivityMonitorStub", new PlaybackActivityMonitorStubImpl.Provider());
                outProviders.put("com.android.server.audio.AudioDeviceInventoryStub", new AudioDeviceInventoryStubImpl.Provider());
                outProviders.put("com.android.server.audio.BtHelperStub", new BtHelperImpl.Provider());
                outProviders.put("com.android.server.audio.FocusRequesterStub", new FocusRequesterStubImpl.Provider());
                outProviders.put("com.android.server.audio.SoundDoseHelperStub", new SoundDoseHelperStubImpl.Provider());
                outProviders.put("com.android.server.audio.MediaFocusControlStub", new MediaFocusControlStubImpl.Provider());
                outProviders.put("com.android.server.audio.AudioServiceStub", new AudioServiceStubImpl.Provider());
                outProviders.put("com.android.server.audio.AudioDeviceBrokerStub", new AudioDeviceBrokerStubImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.autofill.ui.SaveUiStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.autofill.ui.SaveUiStub", new SaveUiStubImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.backup.BackupManagerServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.backup.BackupManagerServiceStub", new BackupManagerServiceInjector.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.backup.SystemBackupAgentStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.backup.SystemBackupAgentStub", new SystemBackupAgentImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.biometrics.sensors.fingerprint.FingerprintServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.biometrics.sensors.fingerprint.FingerprintServiceStub", new FingerprintServiceStubImpl.Provider());
                outProviders.put("com.android.server.biometrics.sensors.fingerprint.HeartRateFingerprintServiceStub", new HeartRateFingerprintServiceStubImpl.Provider());
                outProviders.put("com.android.server.biometrics.sensors.fingerprint.FingerprintServiceInjectorStub", new FingerprintServiceInjectorStubImpl.Provider());
                outProviders.put("com.android.server.biometrics.sensors.fingerprint.FodFingerprintServiceStub", new FodFingerprintServiceStubImpl.Provider());
                outProviders.put("com.android.server.biometrics.sensors.fingerprint.PowerFingerprintServiceStub", new PowerFingerprintServiceStubImpl.Provider());
                outProviders.put("com.android.server.biometrics.sensors.face.FaceServiceStub", new FaceServiceStubImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.camera.CameraActivitySceneStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.camera.CameraOptStub", new CameraOpt.Provider());
                outProviders.put("com.android.server.camera.CameraActivitySceneStub", new CameraActivitySceneMode.Provider());
                outProviders.put("com.android.server.wm.DisplayRotationStub", new DisplayRotationStubImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.clipboard.ClipboardServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.clipboard.ClipboardServiceStub", new ClipboardServiceStubImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.content.ContentServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.content.ContentServiceStub", new ContentServiceStubImpl.Provider());
                outProviders.put("com.android.server.content.SyncSecurityStub", new SyncSecurityStubImpl.Provider());
                outProviders.put("com.android.server.content.SyncManagerStub", new SyncManagerStubImpl.Provider());
                outProviders.put("com.android.server.content.SyncStorageEngineStub", new SyncStorageEngineStubImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.devicepolicy.DevicePolicyManagerServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.devicepolicy.DevicePolicyManagerServiceStub", new DevicePolicyManagerServiceStubImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.devicestate.DeviceStateManagerServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.devicestate.DeviceStateManagerServiceStub", new DeviceStateManagerServiceImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.display.DisplayManagerServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.display.AutomaticBrightnessControllerStub", new AutomaticBrightnessControllerImpl.Provider());
                outProviders.put("com.android.server.display.DisplayAdapterStub", new DisplayAdapterImpl.Provider());
                outProviders.put("com.android.server.display.DisplayManagerServiceStub", new DisplayManagerServiceImpl.Provider());
                outProviders.put("com.android.server.display.HysteresisLevelsStub", new HysteresisLevelsImpl.Provider());
                outProviders.put("com.android.server.display.BrightnessMappingStrategyStub", new BrightnessMappingStrategyImpl.Provider());
                outProviders.put("com.android.server.display.DisplayFeatureManagerServiceStub", new DisplayFeatureManagerServiceImpl.Provider());
                outProviders.put("com.android.server.display.DisplayPowerControllerStub", new DisplayPowerControllerImpl.Provider());
                outProviders.put("com.android.server.display.LocalDisplayAdapterStub", new LocalDisplayAdapterImpl.Provider());
                outProviders.put("com.android.server.display.MiuiBrightnessUtilsStub", new MiuiBrightnessUtilsImpl.Provider());
                outProviders.put("com.android.server.display.mode.DisplayModeDirectorStub", new DisplayModeDirectorImpl.Provider());
                outProviders.put("com.android.server.display.DisplayGroupStub", new DisplayGroupImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.inputmethod.InputMethodManagerServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.inputmethod.MiuiInputMethodStub", new MiuiInputMethodStubImpl.Provider());
                outProviders.put("com.android.server.inputmethod.InputMethodManagerServiceStub", new InputMethodManagerServiceImpl.Provider());
                outProviders.put("com.android.server.wm.MiuiSplitInputMethodStub", new MiuiSplitInputMethodImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.job.JobServiceContextStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.job.JobServiceContextStub", new JobServiceContextImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.lights.LightsManagerStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.lights.LightsManagerStub", new LightsManagerImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.location.gnss.exp.GnssBackgroundUsageOptStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.location.gnss.operators.GnssForOperatorCommonStub", new GnssForCommonOperatorImpl.Provider());
                outProviders.put("com.android.server.location.GnssMockLocationOptStub", new GnssMockLocationOptImpl.Provider());
                outProviders.put("com.android.server.location.gnss.GnssLocationProviderStub", new GnssLocationProviderImpl.Provider());
                outProviders.put("com.android.server.location.MtkGnssPowerSaveStub", new MtkGnssPowerSaveImpl.Provider());
                outProviders.put("com.android.server.location.GnssSmartSatelliteSwitchStub", new GnssSmartSatelliteSwitchImpl.Provider());
                outProviders.put("com.android.server.location.MiuiBlurLocationManagerStub", new MiuiBlurLocationManagerImpl.Provider());
                outProviders.put("com.android.server.location.gnss.GnssEventTrackingStub", new GnssEventTrackingImpl.Provider());
                outProviders.put("com.android.server.location.LocationExtCooperateStub", new LocationExtCooperateImpl.Provider());
                outProviders.put("com.android.server.location.gnss.exp.GnssBackgroundUsageOptStub", new GnssBackgroundUsageOptImpl.Provider());
                outProviders.put("com.android.server.location.gnss.hal.GnssScoringModelStub", new GnssScoringModelImpl.Provider());
                outProviders.put("com.android.server.location.gnss.operators.GnssForKtCustomStub", new GnssForKtCustomImpl.Provider());
                outProviders.put("com.android.server.location.gnss.hal.GnssPowerOptimizeStub", new GnssPowerOptimizeImpl.Provider());
                outProviders.put("com.android.server.location.LocationDumpLogStub", new LocationDumpLogImpl.Provider());
                outProviders.put("com.android.server.location.gnss.operators.GnssForTelcelCustomStub", new GnssForTelcelCustomImpl.Provider());
                outProviders.put("com.android.server.location.provider.LocationProviderManagerStub", new LocationProviderManagerImpl.Provider());
                outProviders.put("com.android.server.location.gnss.gnssSelfRecovery.GnssSelfRecoveryStub", new GnssSelfRecovery.Provider());
                outProviders.put("com.android.server.location.gnss.monitor.LocationSettingsEventMonitorStub", new LocationSettingsEventMonitorImpl.Provider());
                outProviders.put("com.android.server.location.gnss.hal.GnssNativeStub", new GnssNativeImpl.Provider());
                outProviders.put("com.android.server.location.provider.AmapCustomStub", new AmapCustomImpl.Provider());
                outProviders.put("com.android.server.location.gnss.GnssCollectDataStub", new GnssCollectDataImpl.Provider());
                outProviders.put("com.android.server.location.gnss.GnssConfigurationStub", new GnssConfigurationImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.locksettings.LockSettingsStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.locksettings.LockSettingsStub", new LockSettingsImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.logcat.LogcatManagerServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.logcat.LogcatManagerServiceStub", new LogcatManagerServiceImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.media.MediaServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.media.MediaSessionServiceStub", new MediaSessionServiceStubImpl.Provider());
                outProviders.put("com.android.server.media.projection.MediaProjectionManagerServiceStub", new MediaProjectionManagerServiceStubImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.notification.NotificationManagerServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.notification.NotificationManagerServiceStub", new NotificationManagerServiceImpl.Provider());
                outProviders.put("com.android.server.notification.NotificationManagerServiceStub$NMSVersionStub", new NotificationManagerServiceImpl.NMSVersionImpl.Provider());
                outProviders.put("com.android.server.notification.ZenModeStub", new ZenModeStubImpl.Provider());
                outProviders.put("com.android.server.notification.NotificationManagerServiceStub$NRStatusStub", new NotificationManagerServiceImpl.NRStatusImpl.Provider());
                outProviders.put("com.android.server.notification.BarrageListenerServiceStub", new BarrageListenerServiceImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.pm.PackageManagerServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.pm.PerfTimeMonitorStub", new PerfTimeMonitorImpl.Provider());
                outProviders.put("com.android.server.pm.Miui32BitTranslateHelperStub", new Miui32BitTranslateHelper.Provider());
                outProviders.put("com.android.server.pm.UserManagerServiceStub", new UserManagerServiceImpl.Provider());
                outProviders.put("com.android.server.pm.MiuiPreinstallCompressStub", new MiuiPreinstallCompressImpl.Provider());
                outProviders.put("com.android.server.pm.SettingsStub", new SettingsImpl.Provider());
                outProviders.put("com.android.server.pm.PackageManagerServiceUtilsStub", new PackageManagerServiceUtilsImpl.Provider());
                outProviders.put("com.android.server.pm.permission.DefaultPermissionGrantPolicyStub", new MiuiDefaultPermissionGrantPolicy.Provider());
                outProviders.put("com.android.server.pm.MiuiPreinstallHelperStub", new MiuiPreinstallHelper.Provider());
                outProviders.put("com.android.server.pm.PackageManagerServiceStub", new PackageManagerServiceImpl.Provider());
                outProviders.put("com.android.server.pm.permission.PermissionManagerServiceStub", new MiPermissionManagerServiceImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.power.PowerManagerServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.power.PowerManagerServiceStub", new PowerManagerServiceImpl.Provider());
                outProviders.put("com.android.server.power.ShutdownThreadStub", new ShutdownThreadImpl.Provider());
                outProviders.put("com.android.server.power.stats.BatteryStatsManagerStub", new BatteryStatsManagerStubImpl.Provider());
                outProviders.put("com.android.server.power.stats.ScreenPowerCalculatorStub", new ScreenPowerCalculatorImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.print.UserState$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.print.MiuiPrintManagerStub", new MiuiPrintManager.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.sensors.SensorServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.sensors.SensorServiceStub", new SensorServiceImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.statusbar.StatusBarManagerServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.statusbar.StatusBarManagerServiceStub", new StatusBarManagerServiceStubImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.storage.DeviceStorageMonitorServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.storage.DeviceStorageMonitorServiceStub", new DeviceStorageMonitorServiceImpl.Provider());
                outProviders.put("com.android.server.StorageManagerServiceStub", new StorageManagerServiceImpl.Provider());
                outProviders.put("com.android.server.MountServiceIdlerStub", new MountServiceIdlerImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.testharness.TestHarnessModeServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.testharness.TestHarnessModeServiceStub", new TestHarnessModeServiceImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.usage.UsageStatsServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.usage.UsageStatsServiceStub", new UsageStatsServiceImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.usb.MiuiUsbServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.usb.MiuiUsbServiceStub", new MiuiUsbServiceImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.vibrator.VibratorManagerServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.vibrator.VibratorManagerServiceStub", new VibratorManagerServiceImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.wallpaper.WallpaperManagerServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.wallpaper.WallpaperManagerServiceStub", new WallpaperManagerServiceImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.wm.AppResurrectionServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.wm.AppResurrectionServiceStub", new AppResurrectionServiceImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.wm.BoundsCompatControllerStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.wm.ProcessCompatControllerStub", new ProcessCompatController.Provider());
                outProviders.put("com.android.server.wm.BoundsCompatControllerStub", new BoundsCompatController.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.android.server.wm.MultiSenceManagerInternalStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.wm.MultiSenceManagerInternalStub", new MultiSenceManagerInternalStubImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.miui.server.security.SafetyDetectManagerStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.miui.server.security.SafetyDetectManagerStub", new SafetyDetectManagerImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.miui.server.smartpower.SmartPowerServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.am.SmartPowerServiceStub", new SmartPowerService.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.miui.server.sptm.SpeedTestModeServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.am.SpeedTestModeServiceStub", new SpeedTestModeServiceImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.miui.server.xspace.XSpaceManagerServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.android.server.uri.UriGrantsManagerServiceStub", new UriGrantsManagerServiceImpl.Provider());
                outProviders.put("com.miui.server.xspace.XSpaceManagerServiceStub", new XSpaceManagerServiceImpl.Provider());
            }
        });
        list.add(new MiuiStubRegistry.ImplProviderManifest() { // from class: com.xiaomi.mirror.MirrorServiceStub$$
            public final void collectImplProviders(Map<String, MiuiStubRegistry.ImplProvider<?>> outProviders) {
                outProviders.put("com.xiaomi.mirror.MirrorServiceStub", new MirrorServiceImpl.Provider());
            }
        });
    }
}
