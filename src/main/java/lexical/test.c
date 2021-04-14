#include <stdio.h>

int main() {
    // comment
    /* comment */
    /*
     * comment
     */
    /*
     * "string"
     */
    int a /*
    * comment */
            = 10;
    /* comment
    */ int b = 12;
    char c = '/*';
    char d = '*/';
    printf("%d", a);
    printf("%d", b);
    printf("hello//comment");
    printf("/*comment*/");
    printf("Hello, World!\n");
    printf("%c", c);
    printf("%c", d);
    return 0;
}