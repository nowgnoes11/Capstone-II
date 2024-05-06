#include <iostream>
#include <thread>
#include <csignal>
#include <cstring>
#include <unistd.h>
#include <sys/socket.h>
#include <arpa/inet.h>

#define PORT_NUM 12345

using namespace std;

int server_sock, client_sock,valread;
struct sockaddr_in addr;
int opt = 1;
int addr_len = sizeof(addr);
char buf[512];


int main(void) {

	if ((server_sock = socket(AF_INET, SOCK_STREAM, 0)) == 0) {
		cerr << "Socket creation error\n";
		return -1;
	}

	if (setsockopt(server_sock, SOL_SOCKET, SO_REUSEADDR | SO_REUSEPORT, &opt, sizeof(opt)) < 0) {
		cerr << "Socket option setting error\n";
		return -1;
	}
	addr.sin_family = AF_INET;
	addr.sin_addr.s_addr = INADDR_ANY;
	addr.sin_port = htons(PORT_NUM);

	if (bind(server_sock, (struct sockaddr *)&addr, sizeof(addr)) < 0) {
		cerr << "Binding error\n";
		return -1;
	}
	
	if (listen(server_sock, 5) < 0) {
		cerr << "Listen error\n";
		return -1;
	}
	
	if ((client_sock = accept(server_sock, (struct sockaddr *)&addr, (socklen_t *)&addr_len)) < 0) {
		cerr << "Accpetion error\n";
		return -1;
	}
	
	signal(SIGUSR1, [](int signal) {
		cerr << "Alert from Motion Detection\n";
		send(client_sock, "1", strlen("1"), 0);
	});
	
	signal(SIGUSR2, [](int signal) {
		cerr << "Alert from Speech Detection\n";
		send(client_sock, "2", strlen("1"), 0);
	});

	pid_t par_id = getpid();
	//thread t1([&par_id]() { system(("python main_LSTM.py -C Sample_falling.avi -V 1 -P " + to_string(par_id)).c_str()); });
	thread t1([&par_id]() { system(("python main_LSTM.py -C 0 -V 1 -P " + to_string(par_id)).c_str()); });
	thread t2([&par_id]() { system(("python chatgpt.py " + to_string(par_id)).c_str()); });

	t1.join();
	t2.join();

	cout << "main.cpp Fin\n";

	return 0;
}
