#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface AppfastflyApiClient : NSObject

- (instancetype)initWithServiceUrl:(NSString *)serviceUrl apiKey:(NSString *)apiKey;

/// POST JSON to path. Completion called on a background queue.
/// result is nil on any error (network, parse, etc). Never throws.
- (void)post:(NSString *)path
        body:(NSDictionary *)body
  completion:(void (^)(NSDictionary * _Nullable result))completion;

/// DELETE JSON to path. Completion called on a background queue.
/// error is nil on success. Never throws.
- (void)deleteRequest:(NSString *)path
                 body:(NSDictionary *)body
           completion:(void (^)(NSError * _Nullable error))completion;

@end

NS_ASSUME_NONNULL_END
