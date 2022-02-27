CREATE TABLE "STUDENT" (
	"ID" NUMBER ( 32, 0 ) NOT NULL,
	"STUDENT_NAME" VARCHAR2 ( 16 BYTE ),
	"AGE" NUMBER ( 2, 0 ),
	"SEX" NUMBER ( 2, 0 ),
	"CREATE_BY" VARCHAR2 ( 18 BYTE ),
	"CREATE_BY_NAME" VARCHAR2 ( 18 BYTE ),
	"CREATE_TIME" DATE,
	"UPDATE_BY" VARCHAR2 ( 18 BYTE ),
	"UPDATE_BY_NAME" VARCHAR2 ( 18 BYTE ),
	"UPDATE_TIME" DATE,
"IS_DELETE" NUMBER ( 2, 0 ) 
)