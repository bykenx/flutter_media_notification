#import "FlutterMediaNotificationPlugin.h"

@interface FlutterMediaNotificationPlugin()

@property(strong, nonatomic, readonly) FlutterMethodChannel *channel;

@end

@implementation FlutterMediaNotificationPlugin {
    NSMutableDictionary* nowPlayingInfo;
}

+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    
    FlutterMethodChannel* channel = [FlutterMethodChannel methodChannelWithName: @"flutter_media_notification" binaryMessenger: [registrar messenger]];
    
    FlutterMediaNotificationPlugin* instance = [[FlutterMediaNotificationPlugin alloc] initWithRegistrar: registrar channel:channel];
    [registrar addMethodCallDelegate:instance channel:channel];
    [registrar addApplicationDelegate:instance];
}

- (instancetype) initWithRegistrar: (NSObject<FlutterPluginRegistrar>*) registrar channel:(FlutterMethodChannel*)channel {
    self = [self init];
    NSAssert(self, @"super init cannot be nil");
    
    _channel = channel;
    
    nowPlayingInfo = [[NSMutableDictionary alloc] init];
    [self initNowPlayingDefault: nowPlayingInfo];
    [self addHandlers];
    [self disableHandlers];
    
    return self;
}

- (void)dealloc {
    [self removeHandlers];
}

- (void)handleMethodCall:(FlutterMethodCall *)call result:(FlutterResult)result {
    if ([@"showNotification" isEqualToString: call.method]) {
        NSLog(@"show notification");
        [self setNowPlayingMetadata:call.arguments];
        [self enableHandlers];
        result(nil);
    } else if ([@"hideNotification" isEqualToString: call.method]) {
        NSLog(@"hide notification");
        [MPNowPlayingInfoCenter defaultCenter].nowPlayingInfo = nil;
        [self disableHandlers];
        result(nil);
    } else if ([@"updatePlaybackInfo" isEqualToString:call.method]) {
        [self setNowPlayingPlaybackInfo:call.arguments];
        result(nil);
    } else {
        result(FlutterMethodNotImplemented);
    }
}

- (void) initNowPlayingDefault: (NSMutableDictionary*) dict {
    nowPlayingInfo[MPMediaItemPropertyTitle] = @"未知标题";
    nowPlayingInfo[MPMediaItemPropertyArtist] = @"未知艺术家";
    nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = @(0);
    nowPlayingInfo[MPMediaItemPropertyPlaybackDuration] = @(0);
    nowPlayingInfo[MPNowPlayingInfoPropertyDefaultPlaybackRate] = @(1.0);
    nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = @(1.0);
}

#pragma mark - MPNowPlayingInfoCenter

- (void) setNowPlayingMetadata:(NSDictionary*) metadata {
    MPNowPlayingInfoCenter* nowPlayingInfoCenter = [MPNowPlayingInfoCenter defaultCenter];
    
    // need not to set isPlaying nowPlayingInfoCenter.playbackState
    NSString* title = metadata[@"title"];
    NSString* author = metadata[@"author"];
    NSString* cover = metadata[@"cover"];
    
    if(![self isNull:title trim:false]) {
        nowPlayingInfo[MPMediaItemPropertyTitle] = title;
    }
    if(![self isNull:author trim:false]) {
        nowPlayingInfo[MPMediaItemPropertyArtist] = author;
    }
    if(![self isNull:cover trim:true]) {
        // 使用链接中的图片
        NSData* data = [NSData dataWithContentsOfURL:[NSURL URLWithString:cover]];
        MPMediaItemArtwork* artwork = [[MPMediaItemArtwork alloc] initWithImage:[UIImage imageWithData:data]];
        nowPlayingInfo[MPMediaItemPropertyArtwork] = artwork;
    }
    
    nowPlayingInfoCenter.nowPlayingInfo = nowPlayingInfo;
}

- (void) setNowPlayingPlaybackInfo:(NSDictionary*)metadata {
    MPNowPlayingInfoCenter* nowPlayingInfoCenter = [MPNowPlayingInfoCenter defaultCenter];
    
    nowPlayingInfo[MPNowPlayingInfoPropertyElapsedPlaybackTime] = metadata[@"position"];
    nowPlayingInfo[MPMediaItemPropertyPlaybackDuration] = metadata[@"duration"];
    nowPlayingInfo[MPNowPlayingInfoPropertyPlaybackRate] = metadata[@"rate"];

    nowPlayingInfoCenter.nowPlayingInfo = nowPlayingInfo;
}

#pragma mark - MPRemoteCommandCenter
- (void) addHandlers {
    MPRemoteCommandCenter* commandCenter = [MPRemoteCommandCenter sharedCommandCenter];
    [commandCenter.playCommand addTarget: self action: @selector(handlePlay:)];
    [commandCenter.pauseCommand addTarget: self action: @selector(handlePause:)];
    [commandCenter.nextTrackCommand addTarget: self action: @selector(handleNext:)];
    [commandCenter.previousTrackCommand addTarget: self action: @selector(handlePrev:)];
}

- (void) removeHandlers {
    MPRemoteCommandCenter* commandCenter = [MPRemoteCommandCenter sharedCommandCenter];
    [commandCenter.playCommand removeTarget: self];
    [commandCenter.pauseCommand removeTarget: self];
    [commandCenter.nextTrackCommand removeTarget: self];
    [commandCenter.previousTrackCommand removeTarget: self];
}

- (void) enableHandlers {
    MPRemoteCommandCenter* commandCenter = [MPRemoteCommandCenter sharedCommandCenter];
    
    commandCenter.playCommand.enabled = YES;
    commandCenter.pauseCommand.enabled = YES;
    commandCenter.nextTrackCommand.enabled = YES;
    commandCenter.previousTrackCommand.enabled = YES;
}

- (void) disableHandlers {
    MPRemoteCommandCenter* commandCenter = [MPRemoteCommandCenter sharedCommandCenter];
    
    commandCenter.playCommand.enabled = NO;
    commandCenter.pauseCommand.enabled = NO;
    commandCenter.nextTrackCommand.enabled = NO;
    commandCenter.previousTrackCommand.enabled = NO;
}

- (MPRemoteCommandHandlerStatus) handlePlay:(MPRemoteCommandEvent*) event {
    NSLog(@"start playing");
    [_channel invokeMethod:@"play" arguments:nil];
    return MPRemoteCommandHandlerStatusSuccess;
}

- (MPRemoteCommandHandlerStatus) handlePause:(MPRemoteCommandEvent*) event {
    NSLog(@"pause");
    [_channel invokeMethod:@"pause" arguments:nil];
    return MPRemoteCommandHandlerStatusSuccess;
}

- (MPRemoteCommandHandlerStatus) handleNext:(MPRemoteCommandEvent*) event {
    NSLog(@"play next track");
    [_channel invokeMethod:@"next" arguments:nil];
    return MPRemoteCommandHandlerStatusSuccess;
}

- (MPRemoteCommandHandlerStatus) handlePrev:(MPRemoteCommandEvent*) event {
    NSLog(@"play prev track");
    [_channel invokeMethod:@"prev" arguments:nil];
    return MPRemoteCommandHandlerStatusSuccess;
}

- (BOOL) isNull:(NSString*)str trim:(BOOL)trim {
    // nil or NULL
    if(!str) {
        return YES;
    }
    if([str isKindOfClass:[NSNull class]]) {
        return YES;
    }
    if (!str.length) {
        return YES;
    }
    if(trim) {
        NSCharacterSet* set = [NSCharacterSet whitespaceAndNewlineCharacterSet];
        NSString* trimmedStr = [str stringByTrimmingCharactersInSet:set];
        if(!trimmedStr.length) {
            return YES;
        }
    }
    return NO;
}

@end
