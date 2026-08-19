/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.tridium.fox.util.FoxRpcUtil
 *  com.tridium.ndriver.comm.http.NHttpRequest
 *  com.tridium.ndriver.comm.http.NHttpResponse
 *  com.tridium.ndriver.datatypes.BIpAddress
 *  com.tridium.ndriver.util.SfUtil
 *  com.tridium.nvideo.camera.BVideoCamera
 *  com.tridium.nvideo.event.BVideoEventStatus
 *  com.tridium.util.EscUtil
 *  com.tridium.videoDriver.enums.BVideoEventTypesEnum
 *  com.tridium.videoDriver.enums.BVideoPanTiltEnum
 *  com.tridium.videoDriver.enums.BVideoZoomEnum
 *  com.tridium.videoDriver.event.BVideoEvent
 *  com.tridium.videoDriver.security.BDigestAuthMD5Client
 *  com.tridium.videoDriver.videoStream.BIVideoSource
 *  com.tridium.videoDriver.videoStream.BPlaybackParams
 *  com.tridium.videoDriver.videoStream.IVideoDestination
 *  com.tridium.videoDriver.videoStream.IVideoSession
 *  com.tridium.videoDriver.videoStream.IVideoStream
 *  javax.baja.control.BControlPoint
 *  javax.baja.naming.SlotPath
 *  javax.baja.nre.annotations.Facet
 *  javax.baja.nre.annotations.NiagaraAction
 *  javax.baja.nre.annotations.NiagaraActions
 *  javax.baja.nre.annotations.NiagaraProperties
 *  javax.baja.nre.annotations.NiagaraProperty
 *  javax.baja.nre.annotations.NiagaraType
 *  javax.baja.nre.util.Array
 *  javax.baja.rpc.NiagaraRpc
 *  javax.baja.rpc.Transport
 *  javax.baja.rpc.TransportType
 *  javax.baja.security.BPassword
 *  javax.baja.security.BUsernameAndPassword
 *  javax.baja.space.BISpaceNode
 *  javax.baja.status.BStatusValue
 *  javax.baja.sys.Action
 *  javax.baja.sys.BAbsTime
 *  javax.baja.sys.BBoolean
 *  javax.baja.sys.BDynamicEnum
 *  javax.baja.sys.BEnumRange
 *  javax.baja.sys.BFacets
 *  javax.baja.sys.BValue
 *  javax.baja.sys.BajaRuntimeException
 *  javax.baja.sys.Clock
 *  javax.baja.sys.Context
 *  javax.baja.sys.Property
 *  javax.baja.sys.Sys
 *  javax.baja.sys.Type
 *  javax.baja.util.Lexicon
 *  javax.baja.util.LexiconModule
 *  org.baja.ffmpeg.enums.BCodecIdEnum
 *  org.bouncycastle.tls.TlsException
 */
package com.tridium.naxisVideo;

import com.tridium.fox.util.FoxRpcUtil;
import com.tridium.naxisVideo.BAxisVideoNetwork;
import com.tridium.naxisVideo.datatypes.BAxisVideoPanTiltZoomSettings;
import com.tridium.naxisVideo.datatypes.BAxisVideoResolutionSettings;
import com.tridium.naxisVideo.event.BAxisVideoEventCameraExt;
import com.tridium.naxisVideo.event.BAxisVideoEventPointId;
import com.tridium.naxisVideo.event.BAxisVideoEventProxyExt;
import com.tridium.naxisVideo.identify.BAxisVideoCameraDeviceId;
import com.tridium.naxisVideo.messages.AxisRtspVideoStreamRequest;
import com.tridium.naxisVideo.messages.AxisVideoFetchPresetsRequest;
import com.tridium.naxisVideo.messages.AxisVideoMoveToPresetRequest;
import com.tridium.naxisVideo.messages.AxisVideoPanTiltRequest;
import com.tridium.naxisVideo.messages.AxisVideoReadParameterGroupReq;
import com.tridium.naxisVideo.messages.AxisVideoStorePresetRequest;
import com.tridium.naxisVideo.messages.AxisVideoZoomRequest;
import com.tridium.naxisVideo.util.AxisHttpUtil;
import com.tridium.naxisVideo.util.BAxisUserNameAndPassword;
import com.tridium.naxisVideo.util.BAxisVideoCameraInfo;
import com.tridium.naxisVideo.util.BWebAuthScheme;
import com.tridium.ndriver.comm.http.NHttpRequest;
import com.tridium.ndriver.comm.http.NHttpResponse;
import com.tridium.ndriver.datatypes.BIpAddress;
import com.tridium.ndriver.util.SfUtil;
import com.tridium.nvideo.camera.BVideoCamera;
import com.tridium.nvideo.event.BVideoEventStatus;
import com.tridium.util.EscUtil;
import com.tridium.videoDriver.enums.BVideoEventTypesEnum;
import com.tridium.videoDriver.enums.BVideoPanTiltEnum;
import com.tridium.videoDriver.enums.BVideoZoomEnum;
import com.tridium.videoDriver.event.BVideoEvent;
import com.tridium.videoDriver.security.BDigestAuthMD5Client;
import com.tridium.videoDriver.videoStream.BIVideoSource;
import com.tridium.videoDriver.videoStream.BPlaybackParams;
import com.tridium.videoDriver.videoStream.IVideoDestination;
import com.tridium.videoDriver.videoStream.IVideoSession;
import com.tridium.videoDriver.videoStream.IVideoStream;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.control.BControlPoint;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.rpc.NiagaraRpc;
import javax.baja.rpc.Transport;
import javax.baja.rpc.TransportType;
import javax.baja.security.BPassword;
import javax.baja.security.BUsernameAndPassword;
import javax.baja.space.BISpaceNode;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;
import javax.baja.util.LexiconModule;
import javax.net.ssl.SSLException;
import org.baja.ffmpeg.enums.BCodecIdEnum;
import org.bouncycastle.tls.TlsException;

@NiagaraType
@NiagaraProperties(value={@NiagaraProperty(name="videoDeviceId", type="BVideoDeviceId", defaultValue="new BAxisVideoCameraDeviceId()", facets={@Facet(value="SfUtil.incl(SfUtil.MGR_EDIT)")}, override=true), @NiagaraProperty(name="ptzSupport", type="BVideoCameraInfo", defaultValue="new BAxisVideoCameraInfo()", facets={@Facet(value="SfUtil.incl(SfUtil.MGR_EDIT_UNSEEN)")}, override=true), @NiagaraProperty(name="credentials", type="BAxisUserNameAndPassword", defaultValue="new BAxisUserNameAndPassword()", facets={@Facet(value="SfUtil.incl(SfUtil.MGR_EDIT)")}), @NiagaraProperty(name="presetText", type="BEnumRange", defaultValue="BEnumRange.DEFAULT"), @NiagaraProperty(name="panTiltZoomSettings", type="BAxisVideoPanTiltZoomSettings", defaultValue="new BAxisVideoPanTiltZoomSettings()"), @NiagaraProperty(name="resolutionSettings", type="BAxisVideoResolutionSettings", defaultValue="new BAxisVideoResolutionSettings()"), @NiagaraProperty(name="events", type="BAxisVideoEventCameraExt", defaultValue="new BAxisVideoEventCameraExt()"), @NiagaraProperty(name="highCompressionCodec", type="BCodecIdEnum", defaultValue="BCodecIdEnum.ffmpeg_CODEC_ID_MPEG4", facets={@Facet(value="BCodecIdEnum.makeFfmpegCodecFacets()")}), @NiagaraProperty(name="useTcpTransport", type="boolean", defaultValue="true", facets={@Facet(value="SfUtil.incl(SfUtil.MGR_EDIT)")}), @NiagaraProperty(name="useRtspStream", type="boolean", defaultValue="false", facets={@Facet(value="SfUtil.incl(SfUtil.MGR_EDIT)")}), @NiagaraProperty(name="rtspUsername", type="String", defaultValue="root", facets={@Facet(value="SfUtil.incl(SfUtil.MGR_EDIT)")}), @NiagaraProperty(name="rtspPassword", type="BPassword", defaultValue="BPassword.make(\"root\")", facets={@Facet(value="SfUtil.incl(SfUtil.MGR_EDIT)")}), @NiagaraProperty(name="hostName", type="String", defaultValue="", facets={@Facet(value="SfUtil.incl(SfUtil.MGR_EDIT)")}), @NiagaraProperty(name="controlPort", type="int", defaultValue="554"), @NiagaraProperty(name="dataPort", type="int", defaultValue="9000"), @NiagaraProperty(name="webClientHttpPort", type="int", defaultValue="80", facets={@Facet(value="SfUtil.incl(SfUtil.MGR_EDIT_UNSEEN)")}), @NiagaraProperty(name="webClientHttpsPort", type="int", defaultValue="443", facets={@Facet(value="SfUtil.incl(SfUtil.MGR_EDIT_UNSEEN)")}), @NiagaraProperty(name="tokenOverHttps", type="boolean", defaultValue="true", facets={@Facet(value="SfUtil.incl(SfUtil.MGR_EDIT)")}), @NiagaraProperty(name="webAuthScheme", type="BWebAuthScheme", defaultValue="BWebAuthScheme.tokenOrBrowser", facets={@Facet(value="SfUtil.incl(SfUtil.MGR_EDIT)")})})
@NiagaraActions(value={@NiagaraAction(name="moveToPreset", parameterType="BValue", defaultValue="BDynamicEnum.make(0, (BEnumRange)presetText.getDefaultValue())", flags=20, override=true), @NiagaraAction(name="storePreset", parameterType="BValue", defaultValue="BDynamicEnum.make(0, (BEnumRange)presetText.getDefaultValue())", flags=20, override=true), @NiagaraAction(name="autoDetectResolution", flags=4), @NiagaraAction(name="fetchPresets", flags=4)})
public class BAxisVideoCamera
extends BVideoCamera {
    public static final Property videoDeviceId = BAxisVideoCamera.newProperty((int)0, (BValue)new BAxisVideoCameraDeviceId(), (BFacets)SfUtil.incl((String)"ed"));
    public static final Property ptzSupport = BAxisVideoCamera.newProperty((int)0, (BValue)new BAxisVideoCameraInfo(), (BFacets)SfUtil.incl((String)"ed.un"));
    public static final Property credentials = BAxisVideoCamera.newProperty((int)0, (BValue)new BAxisUserNameAndPassword(), (BFacets)SfUtil.incl((String)"ed"));
    public static final Property presetText = BAxisVideoCamera.newProperty((int)0, (BValue)BEnumRange.DEFAULT, null);
    public static final Property panTiltZoomSettings = BAxisVideoCamera.newProperty((int)0, (BValue)new BAxisVideoPanTiltZoomSettings(), null);
    public static final Property resolutionSettings = BAxisVideoCamera.newProperty((int)0, (BValue)new BAxisVideoResolutionSettings(), null);
    public static final Property events = BAxisVideoCamera.newProperty((int)0, (BValue)new BAxisVideoEventCameraExt(), null);
    public static final Property highCompressionCodec = BAxisVideoCamera.newProperty((int)0, (BValue)BCodecIdEnum.ffmpeg_CODEC_ID_MPEG4, (BFacets)BCodecIdEnum.makeFfmpegCodecFacets());
    public static final Property useTcpTransport = BAxisVideoCamera.newProperty((int)0, (boolean)true, (BFacets)SfUtil.incl((String)"ed"));
    public static final Property useRtspStream = BAxisVideoCamera.newProperty((int)0, (boolean)false, (BFacets)SfUtil.incl((String)"ed"));
    public static final Property rtspUsername = BAxisVideoCamera.newProperty((int)0, (String)"root", (BFacets)SfUtil.incl((String)"ed"));
    public static final Property rtspPassword = BAxisVideoCamera.newProperty((int)0, (BValue)BPassword.make((String)"root"), (BFacets)SfUtil.incl((String)"ed"));
    public static final Property hostName = BAxisVideoCamera.newProperty((int)0, (String)"", (BFacets)SfUtil.incl((String)"ed"));
    public static final Property controlPort = BAxisVideoCamera.newProperty((int)0, (int)554, null);
    public static final Property dataPort = BAxisVideoCamera.newProperty((int)0, (int)9000, null);
    public static final Property webClientHttpPort = BAxisVideoCamera.newProperty((int)0, (int)80, (BFacets)SfUtil.incl((String)"ed.un"));
    public static final Property webClientHttpsPort = BAxisVideoCamera.newProperty((int)0, (int)443, (BFacets)SfUtil.incl((String)"ed.un"));
    public static final Property tokenOverHttps = BAxisVideoCamera.newProperty((int)0, (boolean)true, (BFacets)SfUtil.incl((String)"ed"));
    public static final Property webAuthScheme = BAxisVideoCamera.newProperty((int)0, (BValue)BWebAuthScheme.tokenOrBrowser, (BFacets)SfUtil.incl((String)"ed"));
    public static final Action moveToPreset = BAxisVideoCamera.newAction((int)20, (BValue)BDynamicEnum.make((int)0, (BEnumRange)((BEnumRange)presetText.getDefaultValue())), null);
    public static final Action storePreset = BAxisVideoCamera.newAction((int)20, (BValue)BDynamicEnum.make((int)0, (BEnumRange)((BEnumRange)presetText.getDefaultValue())), null);
    public static final Action autoDetectResolution = BAxisVideoCamera.newAction((int)4, null);
    public static final Action fetchPresets = BAxisVideoCamera.newAction((int)4, null);
    public static final Type TYPE = Sys.loadType(BAxisVideoCamera.class);
    private ScheduledExecutorService motionDetectionEventExecutorService = null;
    private IVideoStream motionDetectionVideoStream = null;
    private boolean isVmdSupported = false;
    private String eventListToSubscribe = null;
    public static final Lexicon LEX = Lexicon.make(BAxisVideoCamera.class);
    private static final LexiconModule lexiconModule = LexiconModule.make((String)"naxisVideo");
    private static final Logger axisLog = Logger.getLogger("NAxisVideo");

    public BAxisUserNameAndPassword getCredentials() {
        return (BAxisUserNameAndPassword)this.get(credentials);
    }

    public void setCredentials(BAxisUserNameAndPassword v) {
        this.set(credentials, (BValue)v, null);
    }

    public BEnumRange getPresetText() {
        return (BEnumRange)this.get(presetText);
    }

    public void setPresetText(BEnumRange v) {
        this.set(presetText, (BValue)v, null);
    }

    public BAxisVideoPanTiltZoomSettings getPanTiltZoomSettings() {
        return (BAxisVideoPanTiltZoomSettings)this.get(panTiltZoomSettings);
    }

    public void setPanTiltZoomSettings(BAxisVideoPanTiltZoomSettings v) {
        this.set(panTiltZoomSettings, (BValue)v, null);
    }

    public BAxisVideoResolutionSettings getResolutionSettings() {
        return (BAxisVideoResolutionSettings)this.get(resolutionSettings);
    }

    public void setResolutionSettings(BAxisVideoResolutionSettings v) {
        this.set(resolutionSettings, (BValue)v, null);
    }

    public BAxisVideoEventCameraExt getEvents() {
        return (BAxisVideoEventCameraExt)this.get(events);
    }

    public void setEvents(BAxisVideoEventCameraExt v) {
        this.set(events, (BValue)v, null);
    }

    public BCodecIdEnum getHighCompressionCodec() {
        return (BCodecIdEnum)this.get(highCompressionCodec);
    }

    public void setHighCompressionCodec(BCodecIdEnum v) {
        this.set(highCompressionCodec, (BValue)v, null);
    }

    public boolean getUseTcpTransport() {
        return this.getBoolean(useTcpTransport);
    }

    public void setUseTcpTransport(boolean v) {
        this.setBoolean(useTcpTransport, v, null);
    }

    public boolean getUseRtspStream() {
        return this.getBoolean(useRtspStream);
    }

    public void setUseRtspStream(boolean v) {
        this.setBoolean(useRtspStream, v, null);
    }

    public String getRtspUsername() {
        return this.getString(rtspUsername);
    }

    public void setRtspUsername(String v) {
        this.setString(rtspUsername, v, null);
    }

    public BPassword getRtspPassword() {
        return (BPassword)this.get(rtspPassword);
    }

    public void setRtspPassword(BPassword v) {
        this.set(rtspPassword, (BValue)v, null);
    }

    public String getHostName() {
        return this.getString(hostName);
    }

    public void setHostName(String v) {
        this.setString(hostName, v, null);
    }

    public int getControlPort() {
        return this.getInt(controlPort);
    }

    public void setControlPort(int v) {
        this.setInt(controlPort, v, null);
    }

    public int getDataPort() {
        return this.getInt(dataPort);
    }

    public void setDataPort(int v) {
        this.setInt(dataPort, v, null);
    }

    public int getWebClientHttpPort() {
        return this.getInt(webClientHttpPort);
    }

    public void setWebClientHttpPort(int v) {
        this.setInt(webClientHttpPort, v, null);
    }

    public int getWebClientHttpsPort() {
        return this.getInt(webClientHttpsPort);
    }

    public void setWebClientHttpsPort(int v) {
        this.setInt(webClientHttpsPort, v, null);
    }

    public boolean getTokenOverHttps() {
        return this.getBoolean(tokenOverHttps);
    }

    public void setTokenOverHttps(boolean v) {
        this.setBoolean(tokenOverHttps, v, null);
    }

    public BWebAuthScheme getWebAuthScheme() {
        return (BWebAuthScheme)this.get(webAuthScheme);
    }

    public void setWebAuthScheme(BWebAuthScheme v) {
        this.set(webAuthScheme, (BValue)v, null);
    }

    public void autoDetectResolution() {
        this.invoke(autoDetectResolution, null, null);
    }

    public void fetchPresets() {
        this.invoke(fetchPresets, null, null);
    }

    public Type getType() {
        return TYPE;
    }

    public BAxisVideoCamera() {
        BAxisVideoCameraInfo cameraInfo = (BAxisVideoCameraInfo)this.getPtzSupport();
        cameraInfo.setSupportsFocus(false);
        cameraInfo.setSupportsIris(false);
        cameraInfo.setSupportsMoveToPreset(false);
        cameraInfo.setSupportsPanTilt(false);
        cameraInfo.setSupportsStorePreset(false);
        cameraInfo.setSupportsZoom(false);
    }

    public BAbsTime getCameraTime() {
        return BAbsTime.now();
    }

    public void videoCameraStarted() throws Exception {
        BAxisVideoCameraDeviceId axisCameraDeviceId = (BAxisVideoCameraDeviceId)this.getVideoDeviceId();
        String axisCameraDescription = axisCameraDeviceId.getDescription();
        if (axisCameraDescription.length() == 0) {
            axisCameraDeviceId.setDescription(EscUtil.slot.unescape(this.getName()));
        }
        this.postAsync(new Runnable(){

            @Override
            public void run() {
                BAxisVideoCamera.this.doPing();
            }
        });
        super.videoCameraStarted();
    }

    public BValue getActionParameterDefault(Action action) {
        if (action.equals(moveToPreset) || action.equals(storePreset)) {
            return BDynamicEnum.make((BEnumRange)this.getPresetText());
        }
        return super.getActionParameterDefault(action);
    }

    public Action getMoveToPresetAction() {
        return moveToPreset;
    }

    public Action getStorePresetAction() {
        return storePreset;
    }

    public void initPlaybackParams(BPlaybackParams playbackParams, IVideoSession videoSession) {
    }

    public Type getNetworkType() {
        return BAxisVideoNetwork.TYPE;
    }

    public boolean supportsPlaybackControl() {
        return false;
    }

    protected boolean addPlaybackView() {
        return false;
    }

    public void doAutoDetectResolution() {
        String strRsp;
        String supportedResolutions;
        byte[] rsp = null;
        try {
            AxisVideoReadParameterGroupReq resolutionReq = new AxisVideoReadParameterGroupReq(new BIpAddress(((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getUrlAddress(), ((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getWebPort()), this, "root.Properties.Image.Resolution");
            String passKey = this.getUpdatedVideoKey();
            BAxisUserNameAndPassword usernameAndPassword = new BAxisUserNameAndPassword();
            usernameAndPassword.setUsername(this.getCredentials().getUsername());
            usernameAndPassword.setPassword(BPassword.make((String)passKey));
            resolutionReq.setUsernamePassword(usernameAndPassword);
            resolutionReq.addBasicAuthorization(this.getCredentials().getUsername(), passKey);
            resolutionReq.setRetryCount(5);
            resolutionReq.setResponseTimeOut(20000);
            rsp = this.getAxisVideoNetwork().hcomm().sendRequest((NHttpRequest)resolutionReq).getData();
        }
        catch (Exception e) {
            axisLog.log(Level.SEVERE, e.getMessage(), e);
        }
        if (rsp != null && (supportedResolutions = AxisHttpUtil.inspectLeftHtml("root.Properties.Image.Resolution", strRsp = new String(rsp))) != null) {
            StringTokenizer st = new StringTokenizer(supportedResolutions, ",");
            Array resolutions = new Array(String.class);
            while (st.hasMoreElements()) {
                resolutions.add((Object)st.nextToken());
            }
            int numAvailableResolutions = resolutions.size();
            if (numAvailableResolutions > 0) {
                BAxisVideoResolutionSettings resolutionSettings;
                String lowRes;
                String medRes = lowRes = (String)resolutions.get(0);
                String highRes = lowRes;
                if (numAvailableResolutions > 1) {
                    medRes = lowRes = (String)resolutions.get(1);
                    if (numAvailableResolutions > 2) {
                        lowRes = (String)resolutions.get(2);
                    }
                }
                if ((resolutionSettings = this.getResolutionSettings()).getHigh().length() == 0) {
                    resolutionSettings.setHigh(highRes);
                }
                if (resolutionSettings.getMedium().length() == 0) {
                    resolutionSettings.setMedium(medRes);
                }
                if (resolutionSettings.getLow().length() == 0) {
                    resolutionSettings.setLow(lowRes);
                }
            }
        }
    }

    public void doPing() {
        try {
            NHttpRequest rqst = new NHttpRequest(new BIpAddress(((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getUrlAddress(), ((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getWebPort()), "GET", "/axis-cgi/admin/connection_list.cgi?action=get");
            String passKey = this.getUpdatedVideoKey();
            BAxisUserNameAndPassword usernameAndPassword = new BAxisUserNameAndPassword();
            usernameAndPassword.setUsername(this.getCredentials().getUsername());
            usernameAndPassword.setPassword(BPassword.make((String)passKey));
            rqst.setUsernamePassword((BUsernameAndPassword)usernameAndPassword);
            rqst.addBasicAuthorization(this.getCredentials().getUsername(), passKey);
            rqst.setRetryCount(1);
            rqst.setResponseTimeOut(10000);
            NHttpResponse rsp = this.getAxisVideoNetwork().hcomm().sendRequest(rqst);
            if (rsp != null) {
                this.pingOk();
                this.fetchPresets();
                BAxisVideoResolutionSettings resolutionSettings = this.getResolutionSettings();
                if (resolutionSettings.getHigh().length() == 0 || resolutionSettings.getMedium().length() == 0 || resolutionSettings.getLow().length() == 0) {
                    this.doAutoDetectResolution();
                }
            } else {
                axisLog.log(Level.SEVERE, "No Response for Ping: " + ((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getUrlAddress());
                this.pingFail(LEX.getText("noResponseOnCameraPing"));
            }
        }
        catch (SSLException | TlsException e) {
            axisLog.log(Level.SEVERE, ((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getUrlAddress() + ": " + e.getMessage(), e);
            this.pingFail(LEX.getText("failedCertificateValidationOnCameraPing"));
        }
        catch (Exception e) {
            axisLog.log(Level.SEVERE, e.getMessage(), e);
            this.pingFail(LEX.getText("noConnectionEstablishedOnCameraPing"));
        }
    }

    public void changed(Property prop, Context cx) {
    }

    public BAxisVideoNetwork getAxisVideoNetwork() {
        return (BAxisVideoNetwork)this.getVideoNetwork();
    }

    public void streamToDestination(BPlaybackParams playbackParams, IVideoDestination videoDestination) {
        if (this.getHealth().getDown()) {
            axisLog.log(Level.SEVERE, "Axis Camera " + ((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getUrlAddress() + " status down. Fail to stream to destination.");
            return;
        }
        try {
            if (this.getUseRtspStream()) {
                AxisRtspVideoStreamRequest videoReq = new AxisRtspVideoStreamRequest(new BIpAddress(((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getUrlAddress(), ((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getWebPort()), this, playbackParams);
                String passKey = this.getUpdatedVideoKey();
                BAxisUserNameAndPassword usernameAndPassword = new BAxisUserNameAndPassword();
                usernameAndPassword.setUsername(this.getCredentials().getUsername());
                usernameAndPassword.setPassword(BPassword.make((String)passKey));
                videoReq.setUsernamePassword(usernameAndPassword);
                videoReq.addBasicAuthorization(this.getRtspUsername(), passKey);
                videoReq.setVideoSource((BIVideoSource)this);
                videoReq.setVideoDestination(videoDestination);
                videoReq.setPlaybackParams(playbackParams);
                videoDestination.receiveVideoStream(videoReq.makeVideoStream());
            } else {
                AxisHttpUtil.streamToDestination(this, playbackParams, videoDestination);
            }
        }
        catch (Exception e) {
            axisLog.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public void onMove(BVideoPanTiltEnum panTiltAction) {
        if (panTiltAction != BVideoPanTiltEnum.none) {
            AxisVideoPanTiltRequest rqst = new AxisVideoPanTiltRequest(new BIpAddress(((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getUrlAddress(), ((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getWebPort()), this, panTiltAction);
            String passKey = this.getUpdatedVideoKey();
            BAxisUserNameAndPassword usernameAndPassword = new BAxisUserNameAndPassword();
            usernameAndPassword.setUsername(this.getCredentials().getUsername());
            usernameAndPassword.setPassword(BPassword.make((String)passKey));
            rqst.setUsernamePassword(usernameAndPassword);
            rqst.addBasicAuthorization(this.getCredentials().getUsername(), passKey);
            try {
                this.getAxisVideoNetwork().hcomm().sendRequest((NHttpRequest)rqst);
            }
            catch (Exception e) {
                axisLog.log(Level.SEVERE, e.getMessage(), e);
            }
            this.getAxisVideoNetwork().postAsync(new Runnable(){

                @Override
                public void run() {
                    long keepAlive = Clock.ticks();
                    while (Clock.ticks() - keepAlive < BAxisVideoCamera.this.getControlTiming().getMoveInterval().getMillis()) {
                    }
                    BAxisVideoCamera.this.sendStopMove();
                }
            });
        }
    }

    public void sendStopMove() {
        AxisVideoPanTiltRequest rqst = new AxisVideoPanTiltRequest(new BIpAddress(((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getUrlAddress(), ((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getWebPort()), this, BVideoPanTiltEnum.none);
        String passKey = this.getUpdatedVideoKey();
        BAxisUserNameAndPassword usernameAndPassword = new BAxisUserNameAndPassword();
        usernameAndPassword.setUsername(this.getCredentials().getUsername());
        usernameAndPassword.setPassword(BPassword.make((String)passKey));
        rqst.setUsernamePassword(usernameAndPassword);
        rqst.addBasicAuthorization(this.getCredentials().getUsername(), passKey);
        try {
            this.getAxisVideoNetwork().hcomm().sendRequest((NHttpRequest)rqst);
        }
        catch (Exception e) {
            axisLog.log(Level.SEVERE, "Error in sending stop Pan Tilt request.", e);
        }
    }

    public void onZoom(BVideoZoomEnum zoomAction) {
        if (zoomAction != BVideoZoomEnum.none) {
            AxisVideoZoomRequest rqst = new AxisVideoZoomRequest(new BIpAddress(((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getUrlAddress(), ((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getWebPort()), this, zoomAction);
            String passKey = this.getUpdatedVideoKey();
            BAxisUserNameAndPassword usernameAndPassword = new BAxisUserNameAndPassword();
            usernameAndPassword.setUsername(this.getCredentials().getUsername());
            usernameAndPassword.setPassword(BPassword.make((String)passKey));
            rqst.setUsernamePassword(usernameAndPassword);
            rqst.addBasicAuthorization(this.getCredentials().getUsername(), passKey);
            try {
                this.getAxisVideoNetwork().hcomm().sendRequest((NHttpRequest)rqst);
            }
            catch (Exception e) {
                axisLog.log(Level.SEVERE, "Error in sending Zoom request.", e);
            }
            this.getAxisVideoNetwork().postAsync(new Runnable(){

                @Override
                public void run() {
                    long keepAlive = Clock.ticks();
                    while (Clock.ticks() - keepAlive < BAxisVideoCamera.this.getControlTiming().getZoomInterval().getMillis()) {
                    }
                    BAxisVideoCamera.this.sendStopZoom();
                }
            });
        }
    }

    public void sendStopZoom() {
        AxisVideoZoomRequest rqst = new AxisVideoZoomRequest(new BIpAddress(((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getUrlAddress(), ((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getWebPort()), this, BVideoZoomEnum.none);
        String passKey = this.getUpdatedVideoKey();
        BAxisUserNameAndPassword usernameAndPassword = new BAxisUserNameAndPassword();
        usernameAndPassword.setUsername(this.getCredentials().getUsername());
        usernameAndPassword.setPassword(BPassword.make((String)passKey));
        rqst.setUsernamePassword(usernameAndPassword);
        rqst.addBasicAuthorization(this.getCredentials().getUsername(), passKey);
        try {
            this.getAxisVideoNetwork().hcomm().sendRequest((NHttpRequest)rqst);
        }
        catch (Exception e) {
            axisLog.log(Level.SEVERE, "Error in sending stop Zoom request.");
        }
    }

    public void doMoveToPreset(BValue presetPosition) {
        AxisVideoMoveToPresetRequest rqst = new AxisVideoMoveToPresetRequest(new BIpAddress(((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getUrlAddress(), ((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getWebPort()), this, (BDynamicEnum)presetPosition);
        String passKey = this.getUpdatedVideoKey();
        BAxisUserNameAndPassword usernameAndPassword = new BAxisUserNameAndPassword();
        usernameAndPassword.setUsername(this.getCredentials().getUsername());
        usernameAndPassword.setPassword(BPassword.make((String)passKey));
        rqst.setUsernamePassword(usernameAndPassword);
        rqst.addBasicAuthorization(this.getCredentials().getUsername(), passKey);
        try {
            NHttpResponse nHttpResponse = this.getAxisVideoNetwork().hcomm().sendRequest((NHttpRequest)rqst);
        }
        catch (Exception e) {
            axisLog.log(Level.SEVERE, "Error in sending Move To Preset request.", e);
        }
    }

    public void doStorePreset(BValue presetPosition) {
        AxisVideoStorePresetRequest rqst = new AxisVideoStorePresetRequest(new BIpAddress(((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getUrlAddress(), ((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getWebPort()), this, (BDynamicEnum)presetPosition);
        String passKey = this.getUpdatedVideoKey();
        BAxisUserNameAndPassword usernameAndPassword = new BAxisUserNameAndPassword();
        usernameAndPassword.setUsername(this.getCredentials().getUsername());
        usernameAndPassword.setPassword(BPassword.make((String)passKey));
        rqst.setUsernamePassword(usernameAndPassword);
        rqst.addBasicAuthorization(this.getCredentials().getUsername(), passKey);
        try {
            NHttpResponse rsp = this.getAxisVideoNetwork().hcomm().sendRequest((NHttpRequest)rqst);
            this.fetchPresets();
        }
        catch (Exception e) {
            axisLog.log(Level.SEVERE, "Error in sending Store to Preset request", e);
        }
    }

    public void doFetchPresets() {
        AxisVideoFetchPresetsRequest request = new AxisVideoFetchPresetsRequest(new BIpAddress(((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getUrlAddress(), ((BAxisVideoCameraDeviceId)this.getVideoDeviceId()).getWebPort()), this);
        String passKey = this.getUpdatedVideoKey();
        BAxisUserNameAndPassword usernameAndPassword = new BAxisUserNameAndPassword();
        usernameAndPassword.setUsername(this.getCredentials().getUsername());
        usernameAndPassword.setPassword(BPassword.make((String)passKey));
        request.setUsernamePassword(usernameAndPassword);
        request.addBasicAuthorization(this.getCredentials().getUsername(), passKey);
        try {
            NHttpResponse response = this.getAxisVideoNetwork().hcomm().sendRequest((NHttpRequest)request);
            String responseString = new String(response.getData());
            axisLog.log(Level.FINE, "Get presets response text: " + responseString);
            StringTokenizer tokenizer = new StringTokenizer(responseString, "\n");
            ArrayList<String> list = new ArrayList<String>();
            while (tokenizer.hasMoreElements()) {
                String text = tokenizer.nextToken();
                int presetposnoIndex = text.indexOf("presetposno");
                int equalsSignIndex = text.indexOf("=");
                if (presetposnoIndex == -1 || equalsSignIndex == -1) continue;
                int index = Integer.valueOf(text.substring(presetposnoIndex + "presetposno".length(), equalsSignIndex));
                String presetName = text.substring(equalsSignIndex + 1);
                StringTokenizer presetValueTokenizer = new StringTokenizer(presetName, "\r");
                axisLog.log(Level.FINE, "preset index: " + index);
                if (!presetValueTokenizer.hasMoreElements()) continue;
                String presetTextValue = presetValueTokenizer.nextToken();
                axisLog.log(Level.FINE, "preset value >" + presetTextValue + "<");
                list.add(SlotPath.escape((String)presetTextValue));
            }
            if (null != presetText) {
                String[] presetTextArr = list.toArray(new String[0]);
                this.setPresetText(BEnumRange.make((String[])presetTextArr));
            }
        }
        catch (Exception e) {
            axisLog.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    public String getUpdatedVideoKey() {
        String key = null;
        try {
            key = (String)FoxRpcUtil.doRpc((BISpaceNode)this, (String)"getVideoKeyRpc", (Object[])new Object[0]).orElseThrow(() -> new BajaRuntimeException("RPC call to read password on Axis camera has failed"));
        }
        catch (Exception e) {
            axisLog.log(Level.SEVERE, e.getMessage(), e);
        }
        return key;
    }

    public String getUpdatedRtspVideoKey() {
        String key = null;
        try {
            key = (String)FoxRpcUtil.doRpc((BISpaceNode)this, (String)"getRtspVideoKeyRpc", (Object[])new Object[0]).orElseThrow(() -> new BajaRuntimeException("RPC call to read password on Axis camera has failed"));
        }
        catch (Exception e) {
            axisLog.log(Level.SEVERE, e.getMessage(), e);
        }
        return key;
    }

    @NiagaraRpc(permissions="R", transports={@Transport(type=TransportType.fox)})
    public String getVideoKeyRpc(Context cx) {
        return AccessController.doPrivileged(() -> ((BPassword)this.getCredentials().getPassword()).getValue());
    }

    @NiagaraRpc(permissions="R", transports={@Transport(type=TransportType.fox)})
    public String getRtspVideoKeyRpc(Context cx) {
        return AccessController.doPrivileged(() -> ((BPassword)this.getRtspPassword()).getValue());
    }

    @NiagaraRpc(permissions="r", transports={@Transport(type=TransportType.box)})
    public Map<String, String> getToken(String tokenURI, Context ctx) {
        this.checkState(ctx);
        String username = this.getCredentials().getUsername();
        String password = AccessController.doPrivileged(() -> ((BPassword)this.getCredentials().getPassword()).getValue());
        boolean isSecure = ((BBoolean)ctx.getFacet("isSecure")).getBoolean();
        StringBuilder urlBuilder = new StringBuilder();
        Map<String, String> response = null;
        String rawHostName = this.getHostName();
        String hostName = null;
        hostName = rawHostName.indexOf("axis-media/media.amp") != -1 ? rawHostName.split("/")[0] : rawHostName;
        HashMap<String, String> browserPopupResponse = new HashMap<String, String>();
        browserPopupResponse.put("responseCode", "-1");
        browserPopupResponse.put("token", "-1");
        browserPopupResponse.put("responseMessage", "Browser");
        BWebAuthScheme webAuthScheme = this.getWebAuthScheme();
        if (isSecure && this.getTokenOverHttps()) {
            if (webAuthScheme.equals((Object)BWebAuthScheme.token)) {
                urlBuilder.append("https://").append(hostName).append(":").append(this.getWebClientHttpsPort()).append(tokenURI);
                response = BDigestAuthMD5Client.getHttpsResponse((String)username, (String)password, (String)urlBuilder.toString());
            } else if (webAuthScheme.equals((Object)BWebAuthScheme.browser)) {
                response = browserPopupResponse;
            } else {
                urlBuilder.append("https://").append(hostName).append(":").append(this.getWebClientHttpsPort()).append(tokenURI);
                response = BDigestAuthMD5Client.getHttpsResponse((String)username, (String)password, (String)urlBuilder.toString());
                if (response.get("responseCode").equals("404")) {
                    response = browserPopupResponse;
                }
            }
        } else {
            urlBuilder.append("http://").append(hostName).append(":").append(this.getWebClientHttpPort()).append(tokenURI);
            response = BDigestAuthMD5Client.getResponse((String)username, (String)password, (String)urlBuilder.toString());
            if (((String)response.get("responseCode")).equals("404") && (webAuthScheme.equals((Object)BWebAuthScheme.browser) || webAuthScheme.equals((Object)BWebAuthScheme.tokenOrBrowser))) {
                response = browserPopupResponse;
            }
        }
        axisLog.log(Level.INFO, "URI =" + urlBuilder.toString() + ", GetToken Response = " + response);
        return response;
    }

    public void updateEventPointsForCamera(String actionMessage) {
        axisLog.log(Level.FINE, "Entered into updateEventPointsForCamera with actionMessage = " + actionMessage);
        BAxisVideoEventCameraExt cameraEventsExt = this.getEvents();
        BControlPoint[] eventPoints = cameraEventsExt.getPoints();
        for (int i = 0; i < eventPoints.length; ++i) {
            BVideoEvent videoEvent;
            BAxisVideoEventProxyExt avEventProxy = (BAxisVideoEventProxyExt)eventPoints[i].getProxyExt();
            BAxisVideoEventPointId avEventPointId = avEventProxy.getPointId();
            String actionStart = avEventPointId.getActionStart();
            String actionStop = avEventPointId.getActionStop();
            if (actionMessage.equals(actionStart)) {
                if (avEventPointId.getNiagaraEventType() == BVideoEventTypesEnum.motionStarted) {
                    videoEvent = BVideoEvent.makeMotionStarted();
                } else if (avEventPointId.getNiagaraEventType() == BVideoEventTypesEnum.motionStopped) {
                    videoEvent = BVideoEvent.makeMotionStopped();
                } else {
                    videoEvent = BVideoEvent.makeOffNormalEvent();
                    videoEvent.setDescription(actionMessage);
                }
                avEventProxy.readOk((BStatusValue)new BVideoEventStatus(videoEvent));
                continue;
            }
            if (!actionMessage.equals(actionStop)) continue;
            if (avEventPointId.getNiagaraEventType() == BVideoEventTypesEnum.motionStarted) {
                videoEvent = BVideoEvent.makeMotionStopped();
            } else if (avEventPointId.getNiagaraEventType() == BVideoEventTypesEnum.motionStopped) {
                videoEvent = BVideoEvent.makeMotionStarted();
            } else {
                videoEvent = BVideoEvent.makeNormalEvent();
                videoEvent.setDescription(actionMessage);
            }
            avEventProxy.readOk((BStatusValue)new BVideoEventStatus(videoEvent));
        }
    }

    private void checkState(Context ctx) {
        if (this.getStatus().isDown()) {
            throw new BajaRuntimeException(lexiconModule.getText("axisCameraDown", ctx));
        }
        if (this.getStatus().isDisabled()) {
            throw new BajaRuntimeException(lexiconModule.getText("axisCameraDisabled", ctx));
        }
        if (this.getStatus().isFault()) {
            throw new BajaRuntimeException(lexiconModule.getText("axisCameraInFault", ctx));
        }
    }

    public void updateEventPoint(String actionMessage) {
        this.pingOk();
        this.updateEventPointsForCamera(actionMessage);
    }

    public IVideoStream getMotionDetectionVideoStream() {
        return this.motionDetectionVideoStream;
    }

    public void setMotionDetectionVideoStream(IVideoStream motionDetectionVideoStream) {
        this.motionDetectionVideoStream = motionDetectionVideoStream;
    }

    public ScheduledExecutorService getMotionDetectionEventExecutorService() {
        return this.motionDetectionEventExecutorService;
    }

    public void setMotionDetectionEventExecutorService(ScheduledExecutorService motionDetectionEventExecutorService) {
        this.motionDetectionEventExecutorService = motionDetectionEventExecutorService;
    }

    public boolean isVmdSupported() {
        return this.isVmdSupported;
    }

    public void setVmdSupported(boolean vmdSupported) {
        this.isVmdSupported = vmdSupported;
    }

    public String getEventListToSubscribe() {
        return this.eventListToSubscribe;
    }

    public void setEventListToSubscribe(String eventListToSubscribe) {
        this.eventListToSubscribe = eventListToSubscribe;
    }
}

