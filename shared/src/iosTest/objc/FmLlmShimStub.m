// Test-only stub for the Swift @objc(FmLlmShim) class.
//
// The real implementation lives in the iosApp target (FmLlmShim.swift), which is not part of
// the standalone Kotlin/Native test executable. These stubs provide just enough ObjC linkage
// for `:shared:iosSimulatorArm64Test` to link and launch. They are wired ONLY into the test
// binaries (see shared/build.gradle.kts); production framework/app builds are unaffected.
//
// Inert by design: every method returns a benign failure so that, if a test ever reaches one,
// it observes a deterministic "unavailable" result instead of crashing.
#import <Foundation/Foundation.h>
#import "FmLlmShim.h"

@implementation FmLlmShim

- (instancetype)init {
    self = [super init];
    return self;
}

- (NSString *)availability {
    return @"unavailable:noLocalModel";
}

- (void)generate:(NSString *)prompt
      completion:(void (^)(NSString *_Nullable text, NSString *_Nullable error))completion {
    completion(nil, @"stub: FmLlmShim unavailable in native test binary");
}

- (void)generateWithInstructions:(NSString *)prompt
                    instructions:(NSString *)instructions
                      completion:(void (^)(NSString *_Nullable text, NSString *_Nullable error))completion {
    completion(nil, @"stub: FmLlmShim unavailable in native test binary");
}

- (void)generateJson:(NSString *)prompt
              schema:(NSString *)schema
          completion:(void (^)(NSString *_Nullable text, NSString *_Nullable error))completion {
    completion(nil, @"stub: FmLlmShim unavailable in native test binary");
}

- (void)streamResponseWithInstructions:(NSString *)prompt
                          instructions:(NSString *)instructions
                               onChunk:(void (^)(NSString *cumulativeText))onChunk
                            onComplete:(void (^)(NSString *_Nullable finalText, NSString *_Nullable error))onComplete {
    onComplete(nil, @"stub: FmLlmShim unavailable in native test binary");
}

- (void)cancelStream {
    // No-op: no stream is ever active in the stub.
}

@end
