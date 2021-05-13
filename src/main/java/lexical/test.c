struct student{
    int id ;
    char name ;
}

int x;
int y;
char ch;
float f;
int[10] array;
int[3][3] matrix;

x = 0xf;
x = 0;
y = 10;
ch = '\t';
f = 3.14;

if (x < 5) then
    x = x + 1;
else
    x = x + 2;

x = 0;
y = 0;

while (x < 10) do
    array[x] = x;
    x = x + 1;

array[0] = 0;
matrix[1][1] = 1;

proc int getsum(int a, int b) {
	return a + b;
}

call getsum(x, y);