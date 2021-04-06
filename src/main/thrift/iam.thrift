namespace java bio.terra.common.iam.thrift

struct AuthenticatedUserRequestModel {
    1: string email;
    2: string subjectId;
    3: string token;
    4: optional i64 toRemove = 99;
}

struct AuthenticatedUserRequestModelV2 {
    1: string email;
    2: string subjectId;
    3: string renamedToken;
    // Note we must skip tag 4 for the removed field.
    5: optional string additionalField;
}