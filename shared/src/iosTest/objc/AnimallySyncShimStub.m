// Test-only stub for the Swift @objc(AnimallySyncShim) class.
//
// The real implementation lives in the iosApp target (AnimallySyncShim.swift), which is not
// part of the standalone Kotlin/Native test executable. These stubs provide just enough ObjC
// linkage for `:shared:iosSimulatorArm64Test` to link and launch. They are wired ONLY into the
// test binaries (see shared/build.gradle.kts); production framework/app builds are unaffected.
//
// Inert by design: every async method completes with a benign error so that, if a test ever
// reaches one, it observes a deterministic failure instead of hanging or crashing.
#import <Foundation/Foundation.h>
#import "AnimallySyncShim.h"

@implementation AnimallySyncShim

- (instancetype)init {
    self = [super init];
    return self;
}

- (void)accountStatus:(void (^)(NSString *_Nullable json, NSString *_Nullable error))completion {
    completion(nil, @"stub: AnimallySyncShim unavailable in native test binary");
}

- (void)start:(void (^)(NSString *_Nullable result, NSString *_Nullable error))completion {
    completion(nil, @"stub: AnimallySyncShim unavailable in native test binary");
}

- (void)setEventHandler:(void (^)(NSString *eventJson))handler {
    // No-op: no engine events are ever produced by the stub.
}

- (void)stageRecords:(NSString *)json
          completion:(void (^)(NSString *_Nullable result, NSString *_Nullable error))completion {
    completion(nil, @"stub: AnimallySyncShim unavailable in native test binary");
}

- (void)fetchChanges:(void (^)(NSString *_Nullable result, NSString *_Nullable error))completion {
    completion(nil, @"stub: AnimallySyncShim unavailable in native test binary");
}

- (void)stop {
    // No-op: nothing to tear down in the stub.
}

@end
