#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/// Swift-facing bridge to Apple Foundation Models (iOS 26+).
///
/// Only `NSString` + block callbacks cross the cinterop boundary; every FoundationModels
/// type stays inside the Swift implementation (see `FmLlmShim.swift`). The Kotlin side
/// calls this through a header-only cinterop binding.
@interface FmLlmShim : NSObject

- (instancetype)init NS_DESIGNATED_INITIALIZER;

/// Returns @"available" or @"unavailable:<reason>" where reason is one of:
/// deviceNotEligible, appleIntelligenceNotEnabled, modelNotReady, noLocalModel, unknown.
- (NSString *)availability;

/// Generates a response for `prompt`.
/// `completion` is invoked exactly once with (text, error): on success `text` is non-null and
/// `error` is nil; on failure `text` is nil and `error` describes the failure.
- (void)generate:(NSString *)prompt
      completion:(void (^)(NSString *_Nullable text, NSString *_Nullable error))completion;

/// Generates a response for `prompt` grounded in `instructions` (system prompt).
/// Empty `instructions` behaves like `-generate:completion:`.
/// Same completion contract as `-generate:completion:`.
- (void)generateWithInstructions:(NSString *)prompt
                    instructions:(NSString *)instructions
                      completion:(void (^)(NSString *_Nullable text, NSString *_Nullable error))completion;

/// Generates a structured (JSON) response conforming to `schema`.
/// Same completion contract as `-generate:completion:`.
- (void)generateJson:(NSString *)prompt
              schema:(NSString *)schema
          completion:(void (^)(NSString *_Nullable text, NSString *_Nullable error))completion;

/// Streams a response for `prompt` grounded in `instructions` (system prompt).
/// `onChunk` is invoked once per snapshot with the CUMULATIVE text generated so far
/// (not per-token deltas). `onComplete` is invoked exactly once at the end: on success
/// with (finalText, nil), on failure with (nil, error). Only one stream may be active
/// per shim instance; starting a new one while another is active fails with an error.
- (void)streamResponseWithInstructions:(NSString *)prompt
                          instructions:(NSString *)instructions
                               onChunk:(void (^)(NSString *cumulativeText))onChunk
                            onComplete:(void (^)(NSString *_Nullable finalText, NSString *_Nullable error))onComplete;

/// Cancels the active streaming task started by
/// `-streamResponseWithInstructions:instructions:onChunk:onComplete:`. No-op when idle.
- (void)cancelStream;

@end

NS_ASSUME_NONNULL_END
