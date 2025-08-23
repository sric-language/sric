
#include <assert.h>
#include "WebSocket.h"

//static void reconnect(void* args) {
//    WebSocket* ws = (WebSocket*)args;
//    ws->_connect();
//}

WebSocket::WebSocket() {
}

WebSocket::~WebSocket() {
    if (_runing) {
        close();
        _valid = false;
        _destory();
    }
}

void WebSocket::open(const char* url) {
    if (_runing) {
        //printf("already opened\n");
        //return;
        close();
        _destory();
    }
    _valid = false;
    _runing = true;
    this->url = url;
    _connect();
}

void WebSocket::doConnected() {
    printf("open WebSocket\n");
    _valid = true;
#if __EMSCRIPTEN__
    if (onConnected) onConnected();
#else
    if (sric::call_later) {
        sric::call_later([=] { if (onConnected) onConnected(); });
    }
    else {
        if (onConnected) onConnected();
    }
#endif
}

void WebSocket::doDisconnected() {
    printf("close WebSocket\n");
    _valid = false;
#if __EMSCRIPTEN__
    if (onDisconnected) onDisconnected();
#else
    if (sric::call_later) {
        sric::call_later([=] { if (onDisconnected) onDisconnected(); });
    }
    else {
        if (onDisconnected) onDisconnected();
    }
#endif

// if (ws->autoReconnect) {
//   emscripten_set_timeout(reconnect, 5000, userData);
// }
}

void WebSocket::doReceived(char* data, int size) {
    // We expect to receive the text message before the binary one
    printf("binary data:");
    for (int i = 0; i < size; ++i) {
        printf(" %02X", data[i]);
        //assert(data[i] == i);
    }
    printf("\n");

#if __EMSCRIPTEN__
    if (onReceived) onReceived(data, size);
#else
    if (sric::call_later) {
        sric::call_later([=] { if (onReceived) onReceived(data, size); });
    }
    else {
        if (onReceived) onReceived(data, size);
    }
#endif
}


////////////////////////////////////////////////////////////////////////////////////

#ifdef SRIC_CURL

#include "curl/curl.h"

void WebSocket::close() {
    Msg msg;
    msg.type = 1;
    _channel.write(msg);
    _valid = false;
}

#ifdef SRIC_CURL_CALLBACK
static size_t writecb(char *b, size_t size, size_t nitems, void *p)
{
  WebSocket* socket = (WebSocket*)p;
  CURL *easy = (CURL*)socket->easy;
  //size_t i;
  unsigned int blen = (unsigned int)(nitems * size);
  const struct curl_ws_frame *frame = curl_ws_meta(easy);
  // fprintf(stderr, "Type: %s\n", frame->flags & CURLWS_BINARY ?
  //         "binary" : "text");
  // if(frame->flags & CURLWS_BINARY) {
  //   fprintf(stderr, "Bytes: %u", blen);
  //   for(i = 0; i < nitems; i++)
  //     fprintf(stderr, "%02x ", (unsigned char)b[i]);
  //   fprintf(stderr, "\n");
  // }
  // else
  //   fprintf(stderr, "Text: %.*s\n", (int)blen, b);
  socket->doReceived(b, blen);
  return nitems;
}


static size_t readcb(char *buf, size_t nitems, size_t buflen, void *p)
{
    struct WebSocket * socket = (WebSocket*)p;

    if (socket->sendingMsg.nsent >= socket->sendingMsg.buf.size()) {
        if (socket->channel.size() > 0) {
            socket->sendingMsg = socket->channel.read();
            if (socket->sendingMsg.type == 1) {
                return 0;
            }
        }
    }

    if (socket->sendingMsg.nsent < socket->sendingMsg.buf.size()) {
        auto& msg = socket->sendingMsg;
        size_t len = nitems * buflen;
        size_t left = msg.buf.size() - msg.nsent;
        CURLcode result;

        //if(!ctx->nsent) {
        //  /* On first call, set the FRAME information to be used (it defaults
        //   * to CURLWS_BINARY otherwise). */
        //  result = curl_ws_start_frame((CURL*)ctx->easy, CURLWS_BINARY,
        //                               (curl_off_t)ctx->buf.size());
        //  if(result) {
        //    fprintf(stderr, "error staring frame: %d\n", result);
        //    return CURL_READFUNC_ABORT;
        //  }
        //}
        // 
        //fprintf(stderr, "read(len=%d, left=%d)\n", (int)len, (int)left);
        if (left) {
            if (left < len)
                len = left;
            memcpy(buf, msg.buf.data() + msg.nsent, len);
            msg.nsent += len;
            return len;
        }
    }
    else {

    }
    return 0;
}
#endif

static CURLcode curlSend(CURL* curl, const char* send_payload, int blen)
{
    CURLcode res = CURLE_OK;
    const char* buf = send_payload;
    size_t sent = 0;

    while (blen) {
        res = curl_ws_send(curl, buf, blen, &sent, 0, CURLWS_BINARY);
        if (!res) {
            buf += sent; /* deduct what was sent */
            blen -= sent;
        }
        else if (res == CURLE_AGAIN) {  /* blocked on sending */
            fprintf(stderr, "ws: sent PING blocked, waiting a second\n");
            sric::sleep(1);  /* either select() on socket or max timeout would
                          be good here. */
        }
        else /* real error sending */
            break;
    }

    return res;
}

static CURLcode curlRecv(CURL* curl, WebSocket* socket)
{
    size_t rlen;
    const struct curl_ws_frame* meta;
    std::string buffer;
    CURLcode res;
    size_t offset = 0;

retry:
    res = curl_ws_recv(curl, NULL, 0, &rlen, &meta);
    if (res == CURLE_OK) {
        int flen = meta->bytesleft;
        buffer.resize(flen);

        while (true) {
            res = curl_ws_recv(curl, (char*)buffer.data() + offset, flen - offset, &rlen, &meta);
            offset += rlen;

            if (meta->bytesleft != 0) {
                sric::sleep(1);
            }
            else {
                break;
            }
        }
    }

    if (res == CURLE_OK) {
        if (meta->flags & CURLWS_BINARY || meta->flags & CURLWS_TEXT) {
            /*fprintf(stderr, "ws: received BINARY frame of %u bytes\n",
                (unsigned int)rlen);*/
            socket->doReceived((char*)buffer.data(), rlen);
        }
        else {
            /* some other frame arrived. */
            fprintf(stderr, "ws: received frame of %u bytes rflags %x\n",
                (unsigned int)rlen, meta->flags);
        }
    }
    else if (res == CURLE_AGAIN) {  /* blocked on receiving */
        //fprintf(stderr, "ws: PONG not there yet, waiting a second\n");
        //sric::sleep(1);  /* either select() on socket or max timeout would
        //              be good here. */
        //goto retry;
    }
    else {
        fprintf(stderr, "ws: curl_ws_recv returned %u, received %u\n",
            (unsigned int)res, (unsigned int)rlen);
    }

    return res;
}

/* close the connection */
static void websocket_close(CURL* curl)
{
    size_t sent;
    (void)curl_ws_send(curl, "", 0, &sent, 0, CURLWS_CLOSE);
}

static CURLcode websocket_poll(CURL* curl, WebSocket* socket)
{
    CURLcode res = CURLE_OK;
    int i = 0;
    while (true) {
        if (socket->_channel.size() > 0) {
            auto msg = socket->_channel.read();
            if (msg.type == 1) {
                break;
            }
            res = curlSend(curl, msg.buf.data(), msg.buf.size());
            if (res)
                break;
        }

        res = curlRecv(curl, socket);
        if (res && res != CURLE_AGAIN)
            break;
        sric::sleep(1);
    };

    websocket_close(curl);
    return res;
}

void run_curl(void* arg) {
    WebSocket* socket = (WebSocket*)arg;
    CURLcode res;

    CURL* easy = curl_easy_init();
    socket->_easy = easy;
    if (!easy) {
        fprintf(stderr, "curl_easy_init error\n");
        return;
    }

    curl_easy_setopt(easy, CURLOPT_URL, socket->url.c_str());

    //curl_easy_setopt(easy, CURLOPT_WRITEFUNCTION, writecb);
    //curl_easy_setopt(easy, CURLOPT_WRITEDATA, socket);
    //curl_easy_setopt(easy, CURLOPT_READFUNCTION, readcb);
    //curl_easy_setopt(easy, CURLOPT_READDATA, socket);
    //curl_easy_setopt(easy, CURLOPT_UPLOAD, 1L);

    curl_easy_setopt(easy, CURLOPT_CONNECT_ONLY, 2L);

    /* Perform the request, res gets the return code */
    res = curl_easy_perform(easy);
    /* Check for errors */
    if (res != CURLE_OK) {
        fprintf(stderr, "curl_easy_perform() failed: %s\n",
            curl_easy_strerror(res));
        goto end;
    }
    else {
        socket->doConnected();
        websocket_poll(easy, socket);
    }

end:
    /* always cleanup */
    curl_easy_cleanup(easy);

    socket->doDisconnected();

    //sric::dealloc(socket);
}

void WebSocket::_connect() {
    /*sric::OwnPtr<WebSocket> selfOwn = sric::rawToOwn(this);
    WebSocket* self = selfOwn.take();*/

    _thrd = std::thread(run_curl, this);
}

void WebSocket::send(const char* data, int size) {
    Msg msg;
    msg.buf.assign(data, size);
    _channel.write(msg);
}

void WebSocket::_destory() {
    if (_thrd.joinable()) {
        _thrd.join();
    }
}

#endif

////////////////////////////////////////////////////////////////////////////////////
#ifdef SRIC_LIBHV

#include "WebSocketClient.h"

void WebSocket::close() {
    socket->close();
    valid = false;
}

void WebSocket::_connect() {
    if (socket) {
      return;
    }
    socket = new hv::WebSocketClient();
    socket->onopen = [=](){
        this->doConnected();
    };
    socket->onmessage = [=](const std::string& msg) {
        this->doReceived(msg.data(), msg.size());
    }
    socket->onclose = [=](){
        this->doDisconnected();
    };

    hv::http_headers headers;
    //headers["Origin"] = "http://example.com/";
    open(url, headers);
}

void WebSocket::send(const char* data, int size) {
    socket->send(data, size, WS_OPCODE_BINARY);
}

void WebSocket::_destory() {
    if (socket) {
        delete socket;
        socket = nullptr;
    }
}

#endif

////////////////////////////////////////////////////////////////////////////////////
#if __EMSCRIPTEN__

#include <emscripten/html5.h>
#include <emscripten/websocket.h>

bool WebSocketOpen(int eventType, const EmscriptenWebSocketOpenEvent *e, void *userData) {
  //printf("open(socket=%d, eventType=%d, userData=%p)\n", e->socket, eventType, userData);
  WebSocket* ws = (WebSocket*)userData;
  ws->doConnected();
  //emscripten_websocket_send_utf8_text(e->socket, "hello on the other side");

//   char data[] = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
//   emscripten_websocket_send_binary(e->socket, data, sizeof(data));

  return 0;
}

bool WebSocketClose(int eventType, const EmscriptenWebSocketCloseEvent *e, void *userData) {
  //printf("close(socket=%d, eventType=%d, wasClean=%d, code=%d, reason=%s, userData=%p)\n", e->socket, eventType, e->wasClean, e->code, e->reason, userData);
  WebSocket* ws = (WebSocket*)userData;
  ws->doDisconnected();

  return 0;
}

bool WebSocketError(int eventType, const EmscriptenWebSocketErrorEvent *e, void *userData) {
  printf("error(socket=%d, eventType=%d, userData=%p)\n", e->socket, eventType, userData);
  WebSocket* ws = (WebSocket*)userData;
  ws->_valid = false;
  return 0;
}

bool WebSocketMessage(int eventType, const EmscriptenWebSocketMessageEvent *e, void *userData) {
  //printf("message(socket=%d, eventType=%d, userData=%p data=%p, numBytes=%d, isText=%d)\n", e->socket, eventType, userData, e->data, e->numBytes, e->isText);
  WebSocket* ws = (WebSocket*)userData;
  ws->doReceived((char*)e->data, e->numBytes);
  return 0;
}

void WebSocket::_connect() {
    assert(emscripten_websocket_is_supported());

    if (socket) {
      return;
    }

    EmscriptenWebSocketCreateAttributes attr;
    emscripten_websocket_init_create_attributes(&attr);

    attr.url = url.c_str();
    attr.createOnMainThread = true;
    // We don't really use a special protocol on the server backend in this test,
    // but check that it can be passed.
    attr.protocols = NULL;//"binary";

    EMSCRIPTEN_WEBSOCKET_T socket = emscripten_websocket_new(&attr);
    if (socket) {
        emscripten_websocket_set_onopen_callback(socket, (void*)this, WebSocketOpen);
        emscripten_websocket_set_onclose_callback(socket, (void*)this, WebSocketClose);
        emscripten_websocket_set_onerror_callback(socket, (void*)this, WebSocketError);
        emscripten_websocket_set_onmessage_callback(socket, (void*)this, WebSocketMessage);
    }
    this->socket = (uint64_t)socket;
}

void WebSocket::close() {
    if (socket) {
        emscripten_websocket_close((EMSCRIPTEN_WEBSOCKET_T)socket, 0, 0);
    }
    _valid = false;
}

void WebSocket::_destory() {
    if (socket) {
        emscripten_websocket_delete((EMSCRIPTEN_WEBSOCKET_T)socket);
        socket = 0;
    }
}

void WebSocket::send(const char* data, int size) {
    if (!_valid) {
        printf("Invalide socket\n");
        return;
    }
    emscripten_websocket_send_binary((EMSCRIPTEN_WEBSOCKET_T)socket, (void*)data, size);
}

#endif

//int main() {
//    sric::OwnPtr<WebSocket> socket = sric::makePtr<WebSocket>();
//    socket->open("ws://localhost:8080/websocket");
//
//    sric::sleep(1000);
//    socket->send("a", 1);
//
//    sric::sleep(1000);
//    socket->send("ab", 2);
//
//    sric::sleep(3000);
//    socket->close();
//
//    sric::sleep(1000);
//    return 0;
//}
