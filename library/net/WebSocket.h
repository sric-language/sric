#ifndef SRIC_WEBSOCKET_H_
#define SRIC_WEBSOCKET_H_

#include "sric.h"
#include <functional>

#ifndef __EMSCRIPTEN__
    #define SRIC_CURL
#endif

#ifdef SRIC_CURL
#include <thread>
#endif

namespace sricNet {

class WebSocket {
private:
#ifdef SRIC_CURL
    struct Msg {
        int type = 0;
        std::string buf;
        size_t nsent = 0;
    };
    std::thread _thrd;
public:
    void* _easy = nullptr;
    sric::Channel<Msg> _channel;
private:
    //Msg sendingMsg;
#elif __EMSCRIPTEN__
    uint64_t socket = 0;
#endif
public:
    sric::String url;
private:
    bool autoReconnect = true;
    bool _runing = false;
public:
    bool _valid = false;
    
public:
    std::function<void(char* data, int size)> onReceived;

    std::function<void()> onConnected;
    std::function<void()> onDisconnected;

    WebSocket();
    ~WebSocket();

    void close();
    void open(const char* url);

    void send(const char* data, int size);
    bool isValid() { return _valid; }
    
    void doConnected();
    void doDisconnected();
    void doReceived(char* data, int size);
private:
    void _connect();
    void _destory();
};

}
#endif //WEBSOCKET_H_