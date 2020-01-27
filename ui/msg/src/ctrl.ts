import { MsgData, Contact, Msg, LastMsg, SearchRes, Redraw } from './interfaces';
import notify from 'common/notification';
import * as network from './network';

export default class MsgCtrl {

  data: MsgData;
  searchRes?: SearchRes;

  constructor(data: MsgData, readonly trans: Trans, readonly redraw: Redraw) {
    this.data = data;
    network.websocketHandler(this);
    window.addEventListener('focus', this.setRead);
  };

  openConvo = (userId: string) => {
    network.loadConvo(userId).then(data => {
      this.data = data;
      this.searchRes = undefined;
      this.redraw();
      data.convo && history.replaceState({contact: userId}, '', `/inbox/${data.convo.user.name}`);
    });
  }

  post = (text: string) => {
    if (this.data.convo) {
      network.post(this.data.convo.user.id, text);
      const msg: LastMsg = {
        text,
        user: this.data.me.id,
        date: new Date(),
        read: true
      };
      this.data.convo.msgs.unshift(msg);
      const contact = this.currentContact();
      if (contact) this.addMsg(msg, contact);
      else network.loadContacts().then(data => {
        this.data.contacts = data.contacts;
        this.redraw();
      });
      this.redraw();
    }
  }

  receive = (msg: LastMsg) => {
    const contact = this.findContact(msg.user);
    this.addMsg(msg, contact);
    if (contact) {
      let redrawn = false;
      if (msg.user == this.data.convo?.user.id) {
        this.data.convo.msgs.unshift(msg);
        if (document.hasFocus()) redrawn = this.setRead();
        else this.notify(contact, msg);
      }
      if (!redrawn) this.redraw();
    } else network.loadContacts().then(data => {
      this.data.contacts = data.contacts;
      this.notify(this.findContact(msg.user)!, msg);
      this.redraw();
    });
  }

  private addMsg = (msg: LastMsg, contact?: Contact) => {
    if (contact) {
      contact.lastMsg = msg;
      this.data.contacts = [contact].concat(this.data.contacts.filter(c => c.user.id != contact.user.id));
    }
  }

  private findContact = (userId: string): Contact | undefined =>
    this.data.contacts.filter(c => c.user.id == userId)[0];

  private currentContact = (): Contact | undefined =>
   this.data.convo && this.findContact(this.data.convo.user.id);

  private notify = (contact: Contact, msg: Msg) => {
    notify(() => `${contact.user.name}: ${msg.text}`);
  }

  search = (q: string) => {
    if (q.length > 1) network.search(q).then((res: SearchRes) => {
      this.searchRes = res;
      this.redraw();
    });
    else {
      this.searchRes = undefined;
      this.redraw();
    }
  }

  setRead = () => {
    const msg = this.currentContact()?.lastMsg;
    if (msg && msg.user != this.data.me.id && !msg.read) {
      msg.read = true;
      network.setRead(msg.user);
      this.redraw();
      return true;
    }
    return false;
  }

  delete = () => {
    const userId = this.data.convo?.user.id;
    if (userId) network.del(userId).then(data => {
      this.data = data;
      this.redraw();
      history.replaceState({}, '', '/inbox');
    });
  }

  block = () => {
    const userId = this.data.convo?.user.id;
    if (userId) network.block(userId).then(() => this.openConvo(userId));
  }

  unblock = () => {
    const userId = this.data.convo?.user.id;
    if (userId) network.unblock(userId).then(() => this.openConvo(userId));
  }

  changeBlockBy = (userId: string) => {
    if (userId == this.data.convo?.user.id) this.openConvo(userId);
  }
}
