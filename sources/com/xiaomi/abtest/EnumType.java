package com.xiaomi.abtest;

/* loaded from: classes.dex */
public class EnumType {

    /* loaded from: classes.dex */
    public enum ConditionRelation {
        AND,
        OR
    }

    /* loaded from: classes.dex */
    public enum FlowUnitStatus {
        STATUS_VALID,
        STATUS_INVALID,
        STATUS_DELETED
    }

    /* loaded from: classes.dex */
    public enum FlowUnitType {
        TYPE_UNKNOWN,
        TYPE_DOMAIN,
        TYPE_LAYER,
        TYPE_EXP_CONTAINER,
        TYPE_EXPERIMENT
    }

    /* loaded from: classes.dex */
    public enum ConditionOperator {
        OP_EQ,
        OP_GT,
        OP_GE,
        OP_LT,
        OP_LE,
        OP_IN;

        public static ConditionOperator valueOf(int value) {
            switch (value) {
                case 0:
                    return OP_EQ;
                case 1:
                    return OP_GT;
                case 2:
                    return OP_GE;
                case 3:
                    return OP_LT;
                case 4:
                    return OP_LE;
                case 5:
                    return OP_IN;
                default:
                    return null;
            }
        }
    }

    /* loaded from: classes.dex */
    public enum DiversionType {
        BY_UNKNOWN(0),
        BY_USERID(1),
        BY_SESSIONID(2),
        BY_USERID_DAY(3),
        BY_USERID_WEEK(4),
        BY_USERID_MONTH(5),
        BY_RANDOM(99);

        private int a;

        DiversionType(int id) {
            this.a = id;
        }

        public int getValue() {
            return this.a;
        }
    }
}
