struct type {
    // this is a struct
    char* name;
    char value;
}

int main() {
    long _ = 1000;              // test _ as id
    float abc = 0.9             // test float
    float _abc_1 = +2.9;        // test _abc_1 as id, float
    double d = 1e10;            // test 1e10
    double c = 1e-10;           // test 1e-10
    double b = 1e+10;           // test 1e+10
    int count = 0;              // test 0
    int oct = 00;               // test oct
    int hex = 0xab;             // test hex
    while (count != 100) {      // test !=
        count++;                // test ++
        printf("%d", count);    // test "str", printf
    }
    char s = 's';               // test char
    do {
        printf("%c", count--);  // test --
    } while (count >= 0)        // test >=
    /* test single comment */
    // test single comment
    /*
     * test multiline comment
     */
    // test if else, arithmetic operator
    if (@count < 0 && oct == 0) {   // test <, &&, ==
        count = count + 1;
        count = count * 2;
    } else {
        count = count - 1;
        count = count / 3;
        count = count % 4;
    }
    char c = /*
    test comment in expr
    */ '\n';    // test '\n'
    printf("%c, c);
    char c = 'ab';
    char f = '\pn';
    return 0;
}
/* not closed comment