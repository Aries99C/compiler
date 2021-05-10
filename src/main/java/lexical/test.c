int main() {
    char* ch = '\t';
    int x = 0;
    long y[2][1];
    float z[3];
    float f[1];
    if (x == 1) {
        printf("");
    } else {
        printf("%c", f);
        x = 8;
        z = f;
    }
    while (x > 1 || x < -1) {
        x -= 1;
        y[1][0] = 3.14;
        printf("%f", y[1][0]);
    }
    do {
        func(y);
    } while (y > 0);
    func(x);
}

struct student {
    int id;
    char* name;
}

void func(int x) {
    x = 3;
}